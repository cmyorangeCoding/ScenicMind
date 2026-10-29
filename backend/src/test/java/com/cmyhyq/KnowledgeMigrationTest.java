package com.cmyhyq;

import com.cmyhyq.ai.rag.VectorStoreAdminService;
import com.cmyhyq.pojo.entity.KnowledgeDoc;
import com.cmyhyq.service.KnowledgeDocService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 知识库数据迁移测试类
 * 将 MySQL 中的 knowledge_doc 数据导入到 PostgreSQL 向量库
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
public class KnowledgeMigrationTest {

    @Autowired
    private KnowledgeDocService knowledgeDocService;

    @Autowired
    private VectorStoreAdminService vectorStoreAdminService;

    /**
     * 测试：将所有 MySQL 知识库数据导入到向量库
     */
    @Test
    public void migrateAllKnowledgeToVectorStore() {
        log.info("========== 开始知识库数据迁移 ==========");
        
        // 1. 从 MySQL 查询所有知识文档
        List<KnowledgeDoc> allDocs = knowledgeDocService.list();
        log.info("从 MySQL 查询到 {} 条知识文档", allDocs.size());

        int successCount = 0;
        int failCount = 0;

        // 2. 逐条导入到向量库
        for (KnowledgeDoc doc : allDocs) {
            try {
                log.info("正在导入: id={}, title={}", doc.getId(), doc.getTitle());
                
                vectorStoreAdminService.addDocument(
                        String.valueOf(doc.getId()),
                        doc.getTitle(),
                        doc.getContent(),
                        doc.getCategory(),
                        doc.getTag()
                );
                
                successCount++;
                log.info("✅ 导入成功: {}", doc.getTitle());
                
                // 避免请求过快，稍微延迟
                Thread.sleep(500);
                
            } catch (Exception e) {
                failCount++;
                log.error("❌ 导入失败: id={}, title={}, error={}", 
                        doc.getId(), doc.getTitle(), e.getMessage());
            }
        }

        log.info("========== 知识库数据迁移完成 ==========");
        log.info("总数: {}, 成功: {}, 失败: {}", allDocs.size(), successCount, failCount);
    }


}
