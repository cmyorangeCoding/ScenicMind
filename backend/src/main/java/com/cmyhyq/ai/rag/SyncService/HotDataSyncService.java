package com.cmyhyq.ai.rag.SyncService;

import com.cmyhyq.ai.rag.HotQuestionVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotDataSyncService {

    private final HotQuestionVectorService hotQuestionVectorService;

    /**
     * 触发清理：当通用知识（knowledge_doc）被修改/删除时
     *
     * 【重要】只清理PGVector缓存，不清理MySQL统计表
     * 原因：hot_question表是历史统计数据，记录"哪些问题曾经被频繁问到"
     * 即使知识库被删除了，这个统计信息仍然有价值（用于管理端分析趋势）
     */
    public void onKnowledgeDocChanged(Long knowledgeId) {
        String sourceType = "knowledge_doc";
        String sourceId = String.valueOf(knowledgeId);

        // 1. 删除PGVector中的知识文档本身（type='knowledge'）
        String knowledgeFilter = "type == 'knowledge' AND source_id == '" + sourceId + "'";
        hotQuestionVectorService.deleteByFilter(knowledgeFilter);

        // 2. 删除PGVector中引用该知识的问答缓存（type='qa_cache'）
        String cacheFilter = "type == 'qa_cache' AND source_type == '" + sourceType + "' AND source_id == '" + sourceId + "'";
        hotQuestionVectorService.deleteByFilter(cacheFilter);

        // 3. 不清理MySQL hot_question统计表（保留历史数据）

        log.info("已清理PGVector缓存（知识ID: {}），MySQL统计表保持不变", knowledgeId);
    }

    /**
     * 触发清理：当景点（attraction）被修改/删除时
     *
     * 【重要】只清理PGVector缓存，不清理MySQL统计表
     */
    public void onAttractionChanged(String attrId) {

        String sourceType = "attraction";

        // 1. 删除PGVector中的景点文档本身（type='knowledge'）
        String knowledgeFilter = "type == 'knowledge' AND source_id == '" + attrId + "'";
        hotQuestionVectorService.deleteByFilter(knowledgeFilter);

        // 2. 删除PGVector中引用该景点的问答缓存（type='qa_cache'）
        String cacheFilter = "type == 'qa_cache' AND source_type == '" + sourceType + "' AND source_id == '" + attrId + "'";
        hotQuestionVectorService.deleteByFilter(cacheFilter);

        // 3. 不清理MySQL hot_question统计表（保留历史数据）

        log.info("已清理PGVector缓存（ 景点ID: {}），MySQL统计表保持不变", attrId);
    }
}
