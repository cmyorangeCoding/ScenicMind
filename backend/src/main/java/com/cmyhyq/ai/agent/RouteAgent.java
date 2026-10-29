package com.cmyhyq.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cmyhyq.ai.prompt.RoutePrompt;
import com.cmyhyq.pojo.dto.RouteIntroductionDTO;
import com.cmyhyq.pojo.entity.UserTag;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteAgent {

    private final ChatClient chatClient;
    private final AttractionService attractionService;
    private final ProfileAgent profileAgent;

    //【用户画像推荐规则】---映射表
    private static final Map<String, List<String>> TAG_TO_KEYWORDS = Map.ofEntries(
            Map.entry("19–28岁", List.of("拍照", "视觉震撼", "表演")),
            Map.entry("29–38岁", List.of("文化")),
            Map.entry("39–48岁", List.of("安静", "休闲")),
            Map.entry("49岁以上", List.of("祈福", "佛教文化", "自然", "沉浸式")),
            Map.entry("情侣出行", List.of("拍照")),
            Map.entry("家庭游客", List.of("亲子", "互动", "趣味")),
            Map.entry("多人组团", List.of("轻松")),
            Map.entry("旅游高峰期游客", List.of("热门")),
            Map.entry("高消费", List.of("沉浸式", "体验")),
            Map.entry("低消费", List.of("建筑", "自然")),
            Map.entry("喜欢自然休闲", List.of("自然", "慢游", "轻松", "小众")),
            Map.entry("喜欢历史文化", List.of("休闲", "建筑", "文化")),
            Map.entry("喜欢娱乐", List.of("互动", "体验", "亲子")),
            Map.entry("喜欢地标", List.of("地标", "视觉震撼", "拍照"))
    );


    /**
     * 路线规划核心逻辑
     * 1. 获取景点数据并根据用户画像权重进行初步打分排序
     * 2. 选取 Top 景点的结构化数据传给 AI 决策
     */
    public Flux<String> planRoute(Long userId, String question, String userProfile, String currentAttraction, String chatContext, String timeContext) {
        try {
            // 1. 获取用户带权重的标签
            List<UserTag> userTags = profileAgent.getUserTags(userId);

            // 2. 数据准备：获取所有景点并进行打分排序
            List<Attraction> allAttractions = attractionService.list();
            
            // 计算每个景点的匹配分值
            List<AttractionScore> scoredAttractions = allAttractions.stream()
                    .map(attr -> {
                        double score = calculateScore(attr, userTags);
                        return new AttractionScore(attr, score);
                    })
                    .sorted(Comparator.comparingDouble(AttractionScore::getScore).reversed())
                    .collect(Collectors.toList());

            // 3. 选取前 12 个得分最高的景点（优先展示给 AI）
            String attractionsPool = scoredAttractions.stream()
                    .limit(12)
                    .map(as -> {
                        Attraction attr = as.getAttraction();
                        return String.format(String.format("""
                        景点名称: %s
                        景点类型: %s
                        具体位置: %s
                        游玩亮点: %s
                        核心功能: %s
                        演艺/开放信息: %s
                        是否热门: %s

                        """,
                                attr.getAttrName(),
                                attr.getAttrType(),
                                attr.getLocation(),
                                attr.getHighlights(),
                                attr.getFeatures(),
                                attr.getPerformanceInfo(),
                                attr.getIsHot() == 1 ? "是" : "否"
                        ));
                    })
                    .collect(Collectors.joining("\n"));

            //日志打印Top 12 景点的得分明细
            scoredAttractions.stream()
                    .limit(12)
                    .forEach(as -> log.info("【预筛选Top景点】排名={}, 景点={}, 得分={}, 类型={}",
                            scoredAttractions.indexOf(as) + 1,
                            as.getAttraction().getAttrName(),
                            String.format("%.2f", as.getScore()),
                            as.getAttraction().getAttrType()));




            String tagMappingRules = buildTagMappingPrompt();

            log.info("RouteAgent 启动决策: 用户={}, 当前位置={}, 当前时间={}, 画像标签数={}",
                    userId, currentAttraction, timeContext, userTags.size());

            // 4. AI 决策与流式输出（注意：userProfile 传给 LLM 的依然是纯标签列表，保持简洁）
            return chatClient.prompt()
                    .user(u -> u.text(RoutePrompt.ROUTE_PLANNING_PROMPT)
                            .param("user_tags", userProfile)
                            .param("tag_mapping_rules", tagMappingRules)
                            .param("current_spot", currentAttraction)
                            .param("user_question", question)
                            .param("attractions_info", attractionsPool)
                            .param("chat_context", chatContext)
                            .param("time_context", timeContext)
                            .param("close_time", "17:30")
                            .param("open_time","8:00"))
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("RouteAgent 路线规划决策失败", e);
            return Flux.just("抱歉呀，路线规划服务暂时开小差了，请稍后再试哦~");
        }
    }

    /**
     * 提取路线中的景点列表
     * @param answerText
     * @return
     */
    public RouteIntroductionDTO extractRouteInfo(String answerText) {
        log.info("RouteAgent 开始提取路线信息...");

        // 1. 从内存中获取景点名称列表
        List<String> standardNames = attractionService.getAllAttractionNames();
        String listStr = String.join(", ", standardNames);

        // 2. 动态拼接 Prompt
        String finalPrompt = RoutePrompt.ROUTE_EXTRACTION_PROMPT
                .replace("{attraction_list}", listStr)
                .replace("{count}", String.valueOf(standardNames.size()))
                .replace("{answer_text}", answerText);

        String content = chatClient.prompt()
                .user(finalPrompt)
                .call()
                .content();

        log.debug("RouteAgent 提取结果: {}", content);

        try {
            String json = extractJsonObject(content);
            JSONObject obj = JSONObject.parseObject(json);

            RouteIntroductionDTO dto = new RouteIntroductionDTO();
            dto.setRouteName(obj.getString("route_name"));
            dto.setTotalDuration(obj.getString("total_duration"));

            JSONArray attractionsArr = obj.getJSONArray("attractions");
            if (attractionsArr != null) {
                dto.setAttractions(attractionsArr.toList(String.class));
            }

            return dto;
        } catch (Exception e) {
            log.error("RouteAgent 提取路线信息失败，原始内容: {}", content, e);
            return new RouteIntroductionDTO();
        }
    }




    private String extractJsonObject(String content) {
        if (content == null) {
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

    /**
     * 简单的打分逻辑：根据景点类型与画像标签权重的匹配度计算得分
     */
    private double calculateScore(Attraction attr, List<UserTag> userTags) {
        double score = 0;
        // 热门景点基础加分
        if (attr.getIsHot() != null && attr.getIsHot() == 1) {
            score += 10.0;
        }
        
        // 标签匹配加分：得分 = 权重 * 基础匹配分(5.0)
        for (UserTag userTag : userTags) {
            List<String> keywords = TAG_TO_KEYWORDS.get(userTag.getTag());
            if (keywords != null && attr.getAttrType() != null) {
                boolean matched = keywords.stream()
                        .anyMatch(keyword -> attr.getAttrType().contains(keyword));

                if (matched) {
                    score += 5.0 * userTag.getWeight();
                }
            }
        }
        return score;
    }

    /**
     * 用户画像→景点关键词映射表（prompt）
     * @return
     */
    private String buildTagMappingPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户画像→景点关键词映射表】\n");
        TAG_TO_KEYWORDS.forEach((tag, keywords) -> {
            sb.append(String.format("- %s → %s\n", tag, String.join("、", keywords)));
        });
        return sb.toString();
    }


    /**
     * 内部辅助类：存储景点及其对应的画像匹配分值
     */
    @lombok.Value
    private static class AttractionScore {
        Attraction attraction;
        double score;
    }
}
