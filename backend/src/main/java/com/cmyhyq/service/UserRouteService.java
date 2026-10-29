package com.cmyhyq.service;

import com.cmyhyq.pojo.vo.PageVO;
import com.cmyhyq.pojo.vo.RouteCollectVO;
import com.cmyhyq.pojo.vo.RouteGuideVO;
import com.cmyhyq.pojo.vo.RouteSummaryVO;

public interface UserRouteService {

    /**
     * 用户收藏路线
     * @param userId
     * @param chatId
     * @return
     */
    RouteCollectVO collect(Long userId, Long chatId);

    /**
     * 取消收藏路线
     * @param userId
     * @param routeId
     * @return
     */
    String uncollect(Long userId, Long routeId);

    /**
     * 查询路线列表
     * @param userId
     * @param page
     * @param size
     * @return
     */
    PageVO<RouteSummaryVO> pageUserRoutes(Long userId, int page, int size);


}

