package com.cmyhyq.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreQueryService {

    private final VectorStore vectorStore;

    /**
     * 纯语义检索：QaAgent 使用，完全依赖向量相似度匹配，不做硬性过滤
     * @param query 用户问题
     * @param topK 检索数量
     * @return 检索到的知识内容列表
     */
    public List<String> query(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.6)
                .build();

        log.info("向量库纯语义检索: query='{}', topK={}", query, topK);
        List<Document> searchResults = vectorStore.similaritySearch(request);

        return searchResults.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }
}
