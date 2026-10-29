package com.cmyhyq.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmyhyq.ai.agent.RouteAgent;
import com.cmyhyq.pojo.dto.RouteIntroductionDTO;
import com.cmyhyq.pojo.entity.ChatHistory;
import com.cmyhyq.pojo.entity.RouteCollection;
import com.cmyhyq.pojo.vo.PageVO;
import com.cmyhyq.pojo.vo.RouteCollectVO;
import com.cmyhyq.pojo.vo.RouteSummaryVO;
import com.cmyhyq.service.ChatHistoryService;
import com.cmyhyq.service.RouteCollectionService;
import com.cmyhyq.service.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRouteServiceImpl implements UserRouteService {

    private final ChatHistoryService chatHistoryService;
    private final RouteCollectionService routeCollectionService;
    private final RouteAgent routeAgent;

    /**
     * 收藏路线
     * @param userId
     * @param chatId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RouteCollectVO collect(Long userId, Long chatId) {
        RouteCollection existed = routeCollectionService.lambdaQuery()
                .eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getChatId, chatId)
                .one();
        if (existed != null) {
            RouteCollectVO vo = new RouteCollectVO();
            vo.setRouteId(existed.getId());
            vo.setMessage("已收藏");
            return vo;
        }

        ChatHistory chat = chatHistoryService.getById(chatId);
        if (chat == null) {
            throw new IllegalArgumentException("未找到该对话记录");
        }
        if (!Objects.equals(chat.getUserId(), userId)) {
            throw new IllegalArgumentException("用户身份校验失败");
        }
        if (chat.getAnswer() == null || chat.getAnswer().isBlank()) {
            throw new IllegalArgumentException("对话内容为空，无法收藏");
        }

        RouteIntroductionDTO intro = routeAgent.extractRouteInfo(chat.getAnswer());
        if (intro == null || intro.getAttractions() == null || intro.getAttractions().isEmpty()) {
            throw new IllegalArgumentException("路线解析失败，请稍后重试");
        }

        RouteCollection route = new RouteCollection();
        route.setUserId(userId);
        route.setChatId(chatId);
        route.setRouteName(intro.getRouteName());
        route.setAttractionsJson(JSON.toJSONString(intro.getAttractions()));
        route.setTotalDuration(intro.getTotalDuration());
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        routeCollectionService.save(route);

        RouteCollectVO vo = new RouteCollectVO();
        vo.setRouteId(route.getId());
        vo.setMessage("收藏成功");
        return vo;
    }

    /**
     * 取消收藏
     * @param userId
     * @param routeId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uncollect(Long userId, Long routeId) {
        RouteCollection route = routeCollectionService.getById(routeId);
        if (route == null) {
            return "未找到该路线";
        }
        if (!Objects.equals(route.getUserId(), userId)) {
            throw new IllegalArgumentException("无权限操作该路线");
        }

        routeCollectionService.removeById(routeId);
        return "取消收藏成功";
    }

    /**
     * 查询路线分页列表
     * @param userId
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageVO<RouteSummaryVO> pageUserRoutes(Long userId, int page, int size) {
        Page<RouteCollection> p = routeCollectionService.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<RouteCollection>()
                        .eq(RouteCollection::getUserId, userId)
                        .orderByDesc(RouteCollection::getCreateTime)
        );

        List<RouteSummaryVO> list = p.getRecords().stream()
                .map(rc -> {
                    RouteSummaryVO vo = new RouteSummaryVO();
                    vo.setId(rc.getId());
                    vo.setRouteName(rc.getRouteName());
                    vo.setTotalDuration(rc.getTotalDuration());
                    vo.setCreateTime(rc.getCreateTime());
                    vo.setAttractions(JSON.parseArray(rc.getAttractionsJson(), String.class));
                    return vo;
                })
                .collect(Collectors.toList());

        PageVO<RouteSummaryVO> res = new PageVO<>();
        res.setTotal(p.getTotal());
        res.setList(list);
        return res;
    }

}

