package com.cmyhyq.ai.agent;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmyhyq.ai.prompt.GuidePrompt;
import com.cmyhyq.mapper.AttractionMapper;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.pojo.vo.SpotGuideVO;
import com.cmyhyq.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuideAgent {

    private final ChatClient chatClient;
    private final AttractionService attractionService;

    /**
     * 生成单个景点的个性化讲解重点
     * @param userTags 用户画像标签
     * @param spotName 景点名称
     * @return 讲解重点列表
     */
    public SpotGuideVO generateSpotGuidePoints(String userTags, String spotName) {
        log.info("开始生成景点讲解重点, spotName: {}, userTags: {}", spotName, userTags);


        // 1. 根据景点名称精准查询景点信息
        Attraction attraction = attractionService.getOne(
                new LambdaQueryWrapper<Attraction>().eq(Attraction::getAttrName, spotName)
        );

        // 2.将用户画像标签转为逗号分隔的字符串
        String tagsStr = String.join(", ", userTags);


        // 3. 构建景点信息字符串
        String spotInfo = String.format("""
                景点名称: %s
                景点类型: %s
                游玩亮点: %s
                建筑/景观参数: %s
                核心功能: %s
                文化内涵: %s
                详细介绍: %s
                """,
                attraction.getAttrName(),
                attraction.getAttrType(),
                attraction.getHighlights(),
                attraction.getBuildingInfo(),
                attraction.getFeatures(),
                attraction.getCulture(),
                attraction.getDetail()
        );

        // 4. 手动拼接 Prompt，规避 ST 模板引擎的特殊字符解析问题
        String finalPrompt = GuidePrompt.SPOT_GUIDE_PROMPT
                .replace("{user_tags}", tagsStr)
                .replace("{attraction_info}", spotInfo);

        String content = chatClient.prompt()
                .user(finalPrompt)
                .call()
                .content();

        // 5. 解析并返回结果
        SpotGuideVO vo = new SpotGuideVO();
        vo.setSpotName(spotName);
        vo.setExplanation(content); // 假设 VO 里有个字段存讲解词


        log.info("景点讲解重点生成完成, spotName: {}, 讲解内容: {}", spotName,content);
        return vo;
    }


}
