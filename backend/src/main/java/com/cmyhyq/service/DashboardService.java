package com.cmyhyq.service;

import com.cmyhyq.pojo.vo.DashboardOverviewVO;
import com.cmyhyq.pojo.vo.SatisfactionTrendVO;
import com.cmyhyq.pojo.vo.VisitorReportVO;

import java.util.List;

public interface DashboardService {

    /**
     * 获取满意度趋势（固定近30天）
     * @return 每日满意度平均分列表
     */
    List<SatisfactionTrendVO> getSatisfactionTrend();


    /**
     * 获取数据大屏概览
     * @param period 时间周期：today/week/month/all
     * @return 大屏数据
     */
    DashboardOverviewVO getDashboardOverview(String period);

    /**
     * 获取游客感受度报告（不含AI建议）
     * @param period 时间周期：day/week/month
     */
    VisitorReportVO getVisitorReport(String period);

    /**
     * 单独获取AI生成的服务优化建议
     * @param period 时间周期：day/week/month
     */
    String getAiSuggestions(String period);
}
