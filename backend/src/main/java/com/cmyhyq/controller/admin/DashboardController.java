package com.cmyhyq.controller.admin;



import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.vo.DashboardOverviewVO;
import com.cmyhyq.pojo.vo.SatisfactionTrendVO;
import com.cmyhyq.pojo.vo.VisitorReportVO;
import com.cmyhyq.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取用户满意度趋势（固定近30天）
     */
    @GetMapping("/satisfaction-trend")
    public Result<List<SatisfactionTrendVO>> getSatisfactionTrend() {
        try {
            List<SatisfactionTrendVO> trend = dashboardService.getSatisfactionTrend();
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取满意度趋势失败", e);
            return Result.error("获取满意度趋势失败: " + e.getMessage());
        }
    }


    /**
     * 数据大屏概览
     * @param period 时间周期：today(今日) / week(本周) / month(本月) / all(总榜)
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getDashboardOverview(
            @RequestParam(defaultValue = "today") String period) {
        try {
            DashboardOverviewVO data = dashboardService.getDashboardOverview(period);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取数据大屏失败", e);
            return Result.error("获取数据大屏失败: " + e.getMessage());
        }
    }

    /**
     * 获取游客感受度报告数据
     * @param period 时间周期：day(今日) / week(近7天) / month(近30天)
     */
    @GetMapping("/report/visitor")
    public Result<VisitorReportVO> getVisitorReport(
            @RequestParam(defaultValue = "week") String period) {
        try {
            VisitorReportVO report = dashboardService.getVisitorReport(period);
            return Result.success(report);
        } catch (Exception e) {
            log.error("获取游客感受度报告失败", e);
            return Result.error("获取游客感受度报告失败: " + e.getMessage());
        }
    }

    /**
     * 单独获取AI服务优化建议
     */
    @GetMapping("/report/ai-suggestions")
    public Result<String> getAiSuggestions(
            @RequestParam(defaultValue = "week") String period) {
        try {
            String suggestions = dashboardService.getAiSuggestions(period);
            return Result.success(suggestions);
        } catch (Exception e) {
            log.error("获取AI建议失败", e);
            return Result.error("获取AI建议失败: " + e.getMessage());
        }
    }
}
