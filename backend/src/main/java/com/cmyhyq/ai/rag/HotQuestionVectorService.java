package com.cmyhyq.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotQuestionVectorService {

    private static final double DEFAULT_THRESHOLD = 0.7d;
    private static final int DEFAULT_TOP_K = 1;

    private final VectorStore vectorStore;

    /**
     * 向量相似度匹配（AI 智能语义匹配）
     * 只检索审核通过的缓存（status='approved'）
     * @param query 用户问题
     * @return 匹配结果，未命中返回 null
     */
    public HotQuestionMatch tryMatch(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        // 构建向量检索请求：只查审核通过的缓存
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_THRESHOLD)
                .filterExpression("type == 'qa_cache' AND status == 'approved'")
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        if (results == null || results.isEmpty()) {
            return null;
        }

        Document doc = results.get(0);
        Map<String, Object> metadata = doc.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        String standardQuestion = toTrimmedString(metadata.get("standard_question"));
        String standardAnswer = toTrimmedString(metadata.get("standard_answer"));
        if (standardQuestion == null || standardAnswer == null) {
            return null;
        }

        return new HotQuestionMatch(standardQuestion, standardAnswer);
    }

    /**
     * 写入PGVector（type='qa_cache', status='pending'）
     * @param standardQuestion 标准问题
     * @param standardAnswer 标准答案
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     */
    public void upsert(String standardQuestion, String standardAnswer, String sourceType, String sourceId) {
        if (standardQuestion == null || standardQuestion.isBlank() || standardAnswer == null || standardAnswer.isBlank()) {
            return;
        }

        String originalId = md5Hex(standardQuestion.trim());

        // 先删除向量库中已存在的相同问题（保证唯一）
        deleteByFilter("original_id == '" + originalId + "'");

        // 构建 Document：type='qa_cache', status='pending'
        Document doc = new Document(
                UUID.randomUUID().toString(),
                standardQuestion.trim(),
                Map.of(
                        "type", "qa_cache",
                        "status", "pending",
                        "original_id", originalId,
                        "source_type", sourceType != null ? sourceType : "",
                        "source_id", sourceId != null ? sourceId : "",
                        "standard_question", standardQuestion,
                        "standard_answer", standardAnswer
                )
        );

        vectorStore.add(List.of(doc));
        log.info("PGVector写入成功: type=qa_cache, status=pending, question={}", standardQuestion);
    }



    /**
     * 查询所有待审核的缓存候选（status='pending'）
     * @return 待审核的缓存列表
     */
    public List<HotQuestionMatch> getPendingCaches() {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("dummy")
                            .topK(1000)
                            .filterExpression("type == 'qa_cache' AND status == 'pending'")
                            .build()
            );

            if (results == null || results.isEmpty()) {
                return List.of();
            }

            return results.stream()
                    .map(doc -> {
                        Map<String, Object> metadata = doc.getMetadata();
                        if (metadata == null || metadata.isEmpty()) {
                            return null;
                        }
                        String standardQuestion = toTrimmedString(metadata.get("standard_question"));
                        String standardAnswer = toTrimmedString(metadata.get("standard_answer"));
                        if (standardQuestion == null || standardAnswer == null) {
                            return null;
                        }
                        return new HotQuestionMatch(standardQuestion, standardAnswer);
                    })
                    .filter(match -> match != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询待审核缓存失败", e);
            return List.of();
        }
    }




    /**
     * 审核通过：将 status 从 'pending' 改为 'approved'
     * @param question 问题文本
     */
    public void approve(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        updateStatus(question, "approved");
        log.info("PGVector审核通过: question={}", question);
    }

    /**
     * 审核拒绝：删除该问题
     * @param question 问题文本
     */
    public void reject(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        String originalId = md5Hex(question.trim());
        deleteByFilter("original_id == '" + originalId + "'");
        log.info("PGVector审核拒绝: question={}", question);
    }

    /**
     * 通用过滤删除（优化版）
     * 注意：向量数据库不原生支持 filter delete，采用"查+删"策略
     * 优化点：
     * 1. 分批查询删除，避免单次查询数据量过大
     * 2. 多次轮询确保删除完整（最多3轮）
     * 3. 记录日志便于问题排查
     * 
     * @param filterExpression 过滤表达式
     */
    public void deleteByFilter(String filterExpression) {
        int maxRounds = 3;  // 最多轮询3次，确保删除完整
        int batchSize = 1000;
        
        for (int round = 1; round <= maxRounds; round++) {
            try {
                // 1. 分批查询符合条件的文档
                List<Document> results = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("dummy")
                                .topK(batchSize)
                                .filterExpression(filterExpression)
                                .build()
                );
                
                if (results == null || results.isEmpty()) {
                    // 没有更多数据，删除完成
                    if (round > 1) {
                        log.info("PGVector过滤删除完成（第{}轮）: {}", round, filterExpression);
                    }
                    return;
                }
                
                // 2. 批量删除
                List<String> docIdsToDelete = results.stream()
                        .map(Document::getId)
                        .collect(Collectors.toList());
                vectorStore.delete(docIdsToDelete);
                log.info("PGVector删除第{}轮: 删除{}条", round, docIdsToDelete.size());
                
                // 3. 如果查到的数量少于 batchSize，说明可能已经删除完了
                if (results.size() < batchSize) {
                    log.info("PGVector过滤删除完成: {}", filterExpression);
                    return;
                }
                
            } catch (Exception e) {
                log.error("PGVector过滤删除第{}轮失败", round, e);
                // 继续下一轮重试
            }
        }
        
        log.warn("PGVector过滤删除达到最大轮数{}，可能存在未删除的数据: {}", maxRounds, filterExpression);
    }

    /**
     * 更新Document的status字段
     * 步骤：查找 → 删除旧 → 创建新（带新status）
     * @param question 问题文本
     * @param newStatus 新状态
     */
    private void updateStatus(String question, String newStatus) {
        if (question == null || question.isBlank()) {
            return;
        }
        String originalId = md5Hex(question.trim());

        // 查找所有匹配的问题
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("dummy")
                        .topK(100)
                        .filterExpression("original_id == '" + originalId + "'")
                        .build()
        );

        for (Document doc : results) {
            Map<String, Object> metadata = doc.getMetadata();
            if (metadata == null) {
                continue;
            }

            // 删除旧Document
            vectorStore.delete(List.of(doc.getId()));

            // 创建新Document，更新status
            Document newDoc = new Document(
                    UUID.randomUUID().toString(),
                    doc.getText(),
                    Map.of(
                            "type", toTrimmedString(metadata.get("type")),
                            "status", newStatus,
                            "original_id", toTrimmedString(metadata.get("original_id")),
                            "source_type", toTrimmedString(metadata.get("source_type")),
                            "source_id", toTrimmedString(metadata.get("source_id")),
                            "standard_question", toTrimmedString(metadata.get("standard_question")),
                            "standard_answer", toTrimmedString(metadata.get("standard_answer"))
                    )
            );

            vectorStore.add(List.of(newDoc));
        }
    }

    /**
     * MD5工具：给问题生成唯一ID
     */
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 安全转字符串并trim，空返回null
     */
    private String toTrimmedString(Object v) {
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
