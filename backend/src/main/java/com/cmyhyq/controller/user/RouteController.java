package com.cmyhyq.controller.user;

import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.vo.PageVO;
import com.cmyhyq.pojo.vo.RouteCollectVO;
import com.cmyhyq.pojo.vo.RouteGuideVO;
import com.cmyhyq.pojo.vo.RouteSummaryVO;
import com.cmyhyq.service.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class RouteController {

    private final UserRouteService userRouteService;

    /**
     * 路线收藏功能
     * @param userId
     * @param chatId
     * @return
     */
    @PostMapping("/route/collect")
    public Result<RouteCollectVO> collect(@RequestParam Long userId, @RequestParam Long chatId) {
        try {
            return Result.success(userRouteService.collect(userId, chatId));
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "收藏失败" : e.getMessage());
        }
    }

    /**
     * 取消路线收藏功能
     * @param routeId
     * @param userId
     * @return
     */
    @DeleteMapping("/route/collect/{routeId}")
    public Result<String> uncollect(@PathVariable Long routeId, @RequestParam Long userId) {
        try {
            return Result.success(userRouteService.uncollect(userId, routeId));
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "取消收藏失败" : e.getMessage());
        }
    }

    /**
     * 路线分页列表展示
     * @param userId
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/routes")
    public Result<PageVO<RouteSummaryVO>> pageRoutes(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            return Result.success(userRouteService.pageUserRoutes(userId, page, size));
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "查询失败" : e.getMessage());
        }
    }

}
