package com.cmyhyq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.dto.KnowledgeDocDTO;
import com.cmyhyq.pojo.entity.KnowledgeDoc;
import com.cmyhyq.pojo.vo.KnowledgeDocVO;

public interface KnowledgeDocService extends IService<KnowledgeDoc> {

    /**
     * 管理端：分页查询知识库
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param category 分类筛选（可选）
     * @param tag 标签筛选（可选）
     * @param keyword 关键词搜索（标题/内容）
     * @return 分页结果
     */
    IPage<KnowledgeDocVO> pageForAdmin(Integer pageNum, Integer pageSize, String category, String tag, String keyword);

    /**
     * 管理端：新增知识（MySQL + 向量库双写）
     * @param dto 知识信息
     */
    void addDoc(KnowledgeDocDTO dto);

    /**
     * 管理端：编辑知识（MySQL + 向量库双写）
     * @param dto 知识信息
     */
    void updateDoc(KnowledgeDocDTO dto);

    /**
     * 管理端：删除知识（MySQL + 向量库双删）
     * @param id 知识ID
     */
    void deleteDoc(Long id);

    /**
     * 管理端：批量删除
     * @param ids 知识ID列表
     */
    void batchDelete(java.util.List<Long> ids);


}