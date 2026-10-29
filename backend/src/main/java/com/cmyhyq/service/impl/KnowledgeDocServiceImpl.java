package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.ai.rag.SyncService.HotDataSyncService;
import com.cmyhyq.ai.rag.SyncService.KnowledgeSyncService;
import com.cmyhyq.mapper.KnowledgeDocMapper;
import com.cmyhyq.pojo.dto.KnowledgeDocDTO;
import com.cmyhyq.pojo.entity.KnowledgeDoc;
import com.cmyhyq.pojo.vo.KnowledgeDocVO;
import com.cmyhyq.service.KnowledgeDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeDocServiceImpl extends ServiceImpl<KnowledgeDocMapper, KnowledgeDoc> implements KnowledgeDocService {

    private final KnowledgeSyncService knowledgeSyncService;

    private final HotDataSyncService hotDataSyncService;

    /**
     * 分页查询知识库
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param category 分类筛选（可选）
     * @param tag 标签筛选（可选）
     * @param keyword 关键词搜索（标题/内容）
     * @return
     */
    public IPage<KnowledgeDocVO> pageForAdmin(Integer pageNum, Integer pageSize, String category, String tag, String keyword) {
        Page<KnowledgeDoc> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<KnowledgeDoc> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            queryWrapper.eq(KnowledgeDoc::getCategory, category);
        }
        if (StringUtils.hasText(tag)) {
            queryWrapper.eq(KnowledgeDoc::getTag, tag);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(KnowledgeDoc::getTitle, keyword)
                    .or()
                    .like(KnowledgeDoc::getContent, keyword)
            );
        }

        queryWrapper.orderByDesc(KnowledgeDoc::getUpdateTime);

        IPage<KnowledgeDoc> docPage = this.page(page, queryWrapper);

        IPage<KnowledgeDocVO> voPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        voPage.setRecords(docPage.getRecords().stream().map(doc -> {
            KnowledgeDocVO vo = new KnowledgeDocVO();
            BeanUtils.copyProperties(doc, vo);
            return vo;
        }).toList());

        return voPage;
    }

    /**
     * 管理端：新增知识（MySQL + 向量库双写）
     * @param dto 知识信息
     */
    public void addDoc(KnowledgeDocDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new IllegalArgumentException("内容不能为空");
        }
        if (!StringUtils.hasText(dto.getCategory())) {
            throw new IllegalArgumentException("分类不能为空");
        }

        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setTitle(dto.getTitle());
        doc.setContent(dto.getContent());
        doc.setCategory(dto.getCategory());
        doc.setTag(dto.getTag());
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());

        knowledgeSyncService.saveDoc(doc);
        log.info("管理员新增知识成功: title={}", dto.getTitle());
    }

    /**
     * 管理端：编辑知识（MySQL + 向量库双写）
     * @param dto 知识信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDoc(KnowledgeDocDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("知识ID不能为空");
        }

        KnowledgeDoc existDoc = this.getById(dto.getId());
        if (existDoc == null) {
            throw new IllegalArgumentException("知识不存在");
        }

        if (StringUtils.hasText(dto.getTitle())) {
            existDoc.setTitle(dto.getTitle());
        }
        if (StringUtils.hasText(dto.getContent())) {
            existDoc.setContent(dto.getContent());
        }
        if (StringUtils.hasText(dto.getCategory())) {
            existDoc.setCategory(dto.getCategory());
        }
        if (dto.getTag() != null) {
            existDoc.setTag(dto.getTag());
        }
        existDoc.setUpdateTime(LocalDateTime.now());

        // 1. 调用联动服务：清理关联的热门问答（仅PGVector）
        hotDataSyncService.onKnowledgeDocChanged(dto.getId());

        // 2. 更新知识实体及向量库内容
        knowledgeSyncService.updateDoc(existDoc);
        log.info("管理员编辑知识成功: id={}, title={}", dto.getId(), existDoc.getTitle());
    }


    /**
     * 管理端：删除知识（MySQL + PGVector双删）
     *
     * 【重要】只清理PGVector缓存，不清理MySQL统计表
     * 原因：hot_question表是历史统计数据，记录"哪些问题曾经被频繁问到"
     * 即使知识库被删除了，这个统计信息仍然有价值
     *
     * @param id 知识ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("知识ID不能为空");
        }

        // 1. 调用联动服务：清理PGVector缓存（仅清理缓存，不动MySQL统计表）
        hotDataSyncService.onKnowledgeDocChanged(id);

        // 2. 删除知识实体
        this.removeById(id);

        // 3. 删除向量库中的知识文档
        knowledgeSyncService.deleteDoc(id);

        log.info("管理员删除知识成功: id={}", id);
    }



    /**
     * 管理端：批量删除
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("删除列表不能为空");
        }

        for (Long id : ids) {
            // 1. 先清理PGVector缓存
            hotDataSyncService.onKnowledgeDocChanged(id);
            // 2. 再删除知识本身
            this.removeById(id);
            knowledgeSyncService.deleteDoc(id);
        }
        log.info("管理员批量删除知识成功: 共 {} 条", ids.size());
    }


}
