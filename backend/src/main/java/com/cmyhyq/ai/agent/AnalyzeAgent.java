package com.cmyhyq.ai.agent;

import com.alibaba.fastjson2.JSONObject;
import com.cmyhyq.ai.prompt.AnalyzePrompt;
import com.cmyhyq.mapper.AttractionMapper;
import com.cmyhyq.mapper.KnowledgeDocMapper;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.pojo.entity.ChatHistory;
import com.cmyhyq.pojo.entity.KnowledgeDoc;
import com.cmyhyq.service.AttractionService;
import com.cmyhyq.service.ChatHistoryService;
import com.cmyhyq.service.HotQuestionService;
import com.cmyhyq.service.HotScenicService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeAgent {

    private final ChatClient chatClient;
    private final ChatHistoryService chatHistoryService;
    private final HotQuestionService hotQuestionService;
    private final HotScenicService hotScenicService;
    private final AttractionMapper attractionMapper;
    private final AttractionService attractionService;
    private final KnowledgeDocMapper knowledgeDocMapper;

    /**
     * 异步分析情绪
     * @param chatId
     * @param question
     * @param answer
     */
    @Async
    public void analyzeEmotion(Long chatId, String question, String answer) {
        try {
            // 1. 调用 LLM 分析情感
            String emotion = chatClient.prompt()
                    .user(u -> u.text(AnalyzePrompt.EMOTION_ANALYSIS_PROMPT)
                            .param("user_question", question))
                    .call()
                    .content();

            // 2. 更新数据库
            ChatHistory history = new ChatHistory();
            history.setChatId(chatId);
            history.setEmotion(emotion);
            chatHistoryService.updateById(history);
            log.info("情感分析完成, chatId: {}, 情感: {}", chatId, emotion);
        } catch (Exception e) {
            log.error("情感分析失败, chatId: {}", chatId, e);
        }
    }

    /**
     * 异步方法：AI 智能分析用户问题 → 统计热门话题 + 问答缓存 + 统计热门景点
     * @param question
     * @param answer
     */

    @Async
    public void recordHotTopics(String question, String answer) {
        try {
            log.info("开始热门话题统计, question={}", question);

            // 直接查询数据库获取景点ID和名称
            List<Attraction> attractions = attractionMapper.selectList(
                    new LambdaQueryWrapper<Attraction>().select(Attraction::getAttrId, Attraction::getAttrName)
            );
            String allScenicNames = attractions.stream()
                    .map(a -> a.getAttrId() + "=" + a.getAttrName())
                    .collect(Collectors.joining("、"));

            //  查询知识库的 ID 和 Tag，拼成简单字符串传给 AI
            List<KnowledgeDoc> docs = knowledgeDocMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeDoc>().select(KnowledgeDoc::getId, KnowledgeDoc::getTag)
            );
            String knowledgeMap = docs.stream()
                    .map(d -> d.getId() + "(tag:" + d.getTag() + ")")
                    .collect(Collectors.joining(", "));

            log.info("调用 LLM 分析热门话题...");
            String content = chatClient.prompt()
                    .user(u -> u.text(AnalyzePrompt.HOT_TOPICS_EXTRACT_PROMPT)
                            .param("user_question", question)
                            .param("assistant_answer", answer)
                            .param("all_scenic_names", allScenicNames)
                            .param("knowledge_map", knowledgeMap))
                    .call()
                    .content();

            log.info("LLM 返回内容: {}", content);

            String json = extractJsonObject(content);
            log.info("提取的 JSON: {}", json);

            JSONObject obj = JSONObject.parseObject(json);
            if (obj == null) {
                log.warn("JSON 解析失败");
                return;
            }


            // 1. 热门问题统计（独立逻辑）
            if (Boolean.TRUE.equals(obj.getBoolean("is_hot_topic"))) {
                String standardQuestion = trimToNull(obj.getString("standard_question"));
                String standardAnswer = trimToNull(obj.getString("standard_answer"));
                String sourceType = trimToNull(obj.getString("source_type"));
                String sourceId = trimToNull(obj.getString("source_id"));

                // 分支1：MySQL统计（所有热门问题都统计，只需要问题）
                if (standardQuestion != null) {
                    hotQuestionService.recordHotTopic(standardQuestion, LocalDate.now());
                    log.info("热门问题已统计: question={}", standardQuestion);
                }

                // 分支2：PGVector缓存（只有可缓存的才写入，需要答案和来源）
                Boolean isCacheable = obj.getBoolean("is_cacheable");
                if (Boolean.TRUE.equals(isCacheable) && standardAnswer != null && sourceType != null && sourceId != null) {

                    // 如果是 knowledge_doc 类型，确保 sourceId 是有效的数字
                    if ("knowledge_doc".equals(sourceType) && sourceId != null) {
                        try {
                            long id = Long.parseLong(sourceId);
                            sourceId = String.valueOf(id);
                        } catch (NumberFormatException e) {
                            log.warn("知识库 source_id 不是数字: {}", sourceId);
                            sourceId = null;
                        }
                    }

                    if (sourceId != null) {
                        hotQuestionService.addCacheCandidate(standardQuestion, standardAnswer, sourceType, sourceId);
                        log.info("缓存候选已添加: question={}", standardQuestion);
                    }
                } else {
                    log.info("问题不可缓存: isCacheable={}, hasAnswer={}, sourceType={}, sourceId={}",
                            isCacheable, standardAnswer != null, sourceType, sourceId);
                }
            } else {
                log.info("问题不是热门话题: is_hot_topic=false");
            }



            // 2. 热门景点统计（基于白名单提取，直接精确匹配入库）
            String scenicName = trimToNull(obj.getString("scenic_name"));
            if (scenicName != null) {
                // 根据景点名字精确查询数据库里的景点信息
                Attraction attraction = attractionMapper.selectOne(new LambdaQueryWrapper<Attraction>()
                        .eq(Attraction::getAttrName, scenicName)
                        .last("LIMIT 1"));

                //查到了就统计热度（写入 MySQL）
                if (attraction != null) {
                    hotScenicService.touch(attraction.getAttrId(), scenicName, LocalDate.now());
                    log.info("热门景点已统计: {}", scenicName);
                }
            }
        } catch (Exception e) {
            log.error("热门话题统计失败, question={}, answer={}", question, answer, e);
        }
    }



    /**
     * 生成游记
     * @param userTags 用户画像标签
     * @param spotsInfo 景点详细信息
     * @param visitedSpotsJson 游览路线JSON
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 游记标题和内容
     */
    public TravelNoteContent generateTravelNote(
            String userTags,
            String spotsInfo,
            String visitedSpotsJson,
            String startTime,
            String endTime) {

        log.info("开始生成游记，用户标签长度: {}, 已访问景点数量: {}",
                userTags != null ? userTags.length() : 0,
                visitedSpotsJson != null ? visitedSpotsJson.length() : 0);

        long startTimeMs = System.currentTimeMillis();

        try {
            String content = chatClient.prompt()
                    .user(u -> u.text(AnalyzePrompt.TRAVEL_NOTE_PROMPT)
                            .param("user_tags", userTags)
                            .param("spots_info", spotsInfo)
                            .param("visited_spots", visitedSpotsJson)
                            .param("start_time", startTime)
                            .param("end_time", endTime))
                    .call()
                    .content();

            long endTimeMs = System.currentTimeMillis();
            log.info("游记生成成功，耗时: {}ms，内容长度: {}",
                    (endTimeMs - startTimeMs), content != null ? content.length() : 0);

            // 解析 LLM 返回的 JSON（包含 title 和 content）
            String json = extractJsonObject(content);
            JSONObject obj = JSONObject.parseObject(json);

            TravelNoteContent result = new TravelNoteContent();
            result.setTitle(obj.getString("title"));
            result.setContent(obj.getString("content"));

            return result;

        } catch (Exception e) {
            log.error("生成游记失败，耗时: {}ms，错误: {}",
                    (System.currentTimeMillis() - startTimeMs), e.getMessage(), e);

            // 返回降级内容（优雅降级）
            TravelNoteContent fallback = new TravelNoteContent();
            fallback.setTitle("灵山胜境游记");
            fallback.setContent("根据您的游览记录，我为您生成了一份专属游记。\n\n" +
                    "今天您游览了多个精彩景点，度过了美好的一天。灵山胜境的美景令人难忘，" +
                    "希望这份游记能帮您留住这段美好的回忆！\n\n" +
                    "如需生成更详细的游记，请稍后重试。");

            return fallback;
        }
    }

    @Data
    public static class TravelNoteContent {
        private String title;
        private String content;
    }


    /**
     * 生成管理端服务优化建议（同步调用）
     * @param context 分析上下文数据
     * @return AI生成的优化建议文本
     */
    public String generateManagementSuggestions(String context) {
        try {
            return chatClient.prompt()
                    .user(u -> u.text(AnalyzePrompt.MANAGEMENT_ANALYSIS_PROMPT)
                            .param("context", context))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("生成管理端分析建议失败", e);
            return "抱歉，暂时无法生成智能建议，请稍后重试。";
        }
    }



    /**
     * 从文本中提取 JSON 对象字符串
     * @param content LLM 返回的原始内容
     * @return 提取后的 JSON 字符串，如果未找到则返回 "{}"
     */
    private String extractJsonObject(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }
        String s = content.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return "{}";
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

}
