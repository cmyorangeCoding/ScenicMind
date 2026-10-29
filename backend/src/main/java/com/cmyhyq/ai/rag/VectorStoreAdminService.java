package com.cmyhyq.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreAdminService {

    private final VectorStore vectorStore;
    private final DocumentSplitter documentSplitter;

    /**
     * 添加文档到向量库
     * @param id 文档ID（MySQL中的主键）
     * @param title 文档标题
     * @param content 文档内容
     * @param category 文档类别
     * @param tag 文档标签
     */
    public void addDocument(String id, String title, String content, String category, String tag) {
        List<String> chunks = documentSplitter.splitSmart(content);
        List<Document> documents = chunks.stream()
                .map(chunk -> {
                    String docId = UUID.randomUUID().toString();
                    return new Document(
                            docId,
                            chunk,
                            Map.of(
                                    "original_id", id,
                                    "title", title,
                                    "category", category,
                                    "tag", tag
                            )
                    );
                })
                .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("文档已添加到向量库: id={}, title={}, 分块数={}", id, title, chunks.size());
    }

    /**
     * 更新向量库中的文档
     * @param id 文档ID
     * @param title 文档标题
     * @param content 文档内容
     * @param category 文档类别
     * @param tag 文档标签
     */
    public void updateDocument(String id, String title, String content, String category, String tag) {
        deleteDocument(id);
        addDocument(id, title, content, category, tag);
        log.info("文档已更新到向量库: id={}, title={}", id, title);
    }

    /**
     * 从向量库中删除文档及其所有分块
     * @param id MySQL 中的文档 ID
     */
    public void deleteDocument(String id) {
        try {
            // 1. 用一个固定的占位符查询文本，避免空字符串报错
            String dummyQuery = "dummy_query_for_filter_only";
            // 2. 先查询所有匹配的分块ID
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(dummyQuery)
                            .topK(10000)
                            .filterExpression("original_id == '" + id + "'")
                            .build()
            );

            if (!results.isEmpty()) {
                List<String> docIdsToDelete = results.stream()
                        .map(Document::getId)
                        .collect(Collectors.toList());
                vectorStore.delete(docIdsToDelete);
                log.info("从向量库中删除了 {} 个分块, original_id={}", docIdsToDelete.size(), id);
            }
        } catch (Exception e) {
            log.error("从向量库删除文档失败: id={}", id, e);
            // 兜底：如果查询失败，直接尝试删除所有可能的ID（这里可以根据你的数据库特性调整）
            // 对于pgvector等支持批量删除的数据库，可以考虑直接用SQL删除
        }
    }
}