package com.cmyhyq.ai.rag.SyncService;

import com.cmyhyq.ai.rag.VectorStoreAdminService;
import com.cmyhyq.mapper.KnowledgeDocMapper;
import com.cmyhyq.pojo.entity.KnowledgeDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSyncService {

    private final KnowledgeDocMapper knowledgeDocMapper; // 替换为 Mapper，解开循环依赖
    private final VectorStoreAdminService vectorStoreAdminService;

    /**
     * 新增：MySQL + 向量库双写
     * 注意：不标注事务，事务由外层 Service 统一管理
     */
    public void saveDoc(KnowledgeDoc doc) {
        try {
            // 1. 保存 MySQL
            knowledgeDocMapper.insert(doc);

            // 2. 同步向量库
            vectorStoreAdminService.addDocument(
                    String.valueOf(doc.getId()),
                    doc.getTitle(),
                    doc.getContent(),
                    doc.getCategory(),
                    doc.getTag()
            );

            log.info("✅ 知识文档新增并同步向量库成功: id={}", doc.getId());
        } catch (Exception e) {
            log.error("❌ 新增文档同步失败: id={}", doc.getId(), e);
            throw new RuntimeException("文档新增失败", e);
        }
    }

    /**
     * 更新：MySQL + 向量库
     * 注意：不标注事务，事务由外层 Service 统一管理
     */
    public void updateDoc(KnowledgeDoc doc) {
        try {
            // 1. 更新 MySQL
            knowledgeDocMapper.updateById(doc);

            // 2. 更新向量库
            vectorStoreAdminService.updateDocument(
                    String.valueOf(doc.getId()),
                    doc.getTitle(),
                    doc.getContent(),
                    doc.getCategory(),
                    doc.getTag()
            );

            log.info("✅ 知识文档更新并同步向量库成功: id={}", doc.getId());
        } catch (Exception e) {
            log.error("❌ 更新文档同步失败: id={}", doc.getId(), e);
            throw new RuntimeException("文档更新失败", e);
        }
    }

    /**
     * 删除：MySQL + 向量库
     * 注意：不标注事务，事务由外层 Service 统一管理
     */
    public void deleteDoc(Long id) {
        try {
            // 1. 删除 MySQL
            knowledgeDocMapper.deleteById(id);

            // 2. 删除向量库
            vectorStoreAdminService.deleteDocument(String.valueOf(id));

            log.info("✅ 知识文档删除并同步向量库成功: id={}", id);
        } catch (Exception e) {
            log.error("❌ 删除文档同步失败: id={}", id, e);
            throw new RuntimeException("文档删除失败", e);
        }
    }
}
