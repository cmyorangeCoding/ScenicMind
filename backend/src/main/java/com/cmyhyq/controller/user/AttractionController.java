package com.cmyhyq.controller.user;

import com.cmyhyq.ai.agent.GuideAgent;
import com.cmyhyq.common.Result;
import com.cmyhyq.mapper.UserMapper;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.pojo.vo.AttractionSimpleVO;
import com.cmyhyq.pojo.vo.SpotGuideVO;
import com.cmyhyq.service.AttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;
    private final GuideAgent guideAgent; // 直接注入 Agent
    private final UserMapper userMapper; // 用于获取用户标签

    /**
     * 查询所有景点相关信息（仅返回名称和文化内涵）
     * @return 景点简化信息列表
     */
    @GetMapping("/attractionsInfo")
    public Result<List<AttractionSimpleVO>> getAllAttractions() {
        try {
            List<Attraction> attractions = attractionService.list();

            List<AttractionSimpleVO> simpleList = attractions.stream()
                    .map(attr -> {
                        AttractionSimpleVO vo = new AttractionSimpleVO();
                        vo.setAttrName(attr.getAttrName());
                        vo.setCulture(attr.getCulture());
                        return vo;
                    })
                    .collect(Collectors.toList());

            return Result.success(simpleList);
        } catch (Exception e) {
            return Result.error("查询景点信息失败：" + e.getMessage());
        }
    }

    /**
     * 个性化生成景点的重点讲解
     * @param spotName
     * @param userId
     * @return
     */
    @GetMapping("/spot-guide")
    public Result<SpotGuideVO> getSpotGuide(
            @RequestParam String spotName,
            @RequestParam Long userId) {
        try {
            // 1. 获取用户标签逻辑移到 Controller 或 Service 外部
            User user = userMapper.selectById(userId);
            String userTags = "无特殊偏好";
            if (user != null && user.getTagsJson() != null && !user.getTagsJson().isBlank()) {
                // 这里可以复用你之前在 Service 里的解析逻辑，或者简单传入 JSON
                userTags = user.getTagsJson();
            }

            // 2. 直接调用 Agent
            SpotGuideVO vo = guideAgent.generateSpotGuidePoints(userTags, spotName);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "生成讲解重点失败" : e.getMessage());
        }
    }

}
