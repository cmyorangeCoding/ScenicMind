package com.cmyhyq.service.impl;

import com.cmyhyq.ai.agent.AnalyzeAgent;
import com.cmyhyq.mapper.ChatHistoryMapper;
import com.cmyhyq.mapper.ComplaintMapper;
import com.cmyhyq.mapper.HotQuestionMapper;
import com.cmyhyq.mapper.HotScenicMapper;
import com.cmyhyq.pojo.vo.*;
import com.cmyhyq.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ChatHistoryMapper chatHistoryMapper;
    private final HotQuestionMapper hotQuestionMapper;
    private final HotScenicMapper hotScenicMapper;
    private final ComplaintMapper complaintMapper;
    private final AnalyzeAgent analyzeAgent;


    /**
     * 获得近30天情感统计趋势
     * @return
     */
    public List<SatisfactionTrendVO> getSatisfactionTrend() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(30);
        return chatHistoryMapper.selectSatisfactionTrend(startTime, endTime);
    }


    // ... existing code ...

    /**
     * 获取数据大屏概览
     * @param period 时间周期：today/week/month/all
     * @return
     */
    public DashboardOverviewVO getDashboardOverview(String period) {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        // 1. 统计活跃人数和对话次数（chat_history 始终需要时间范围）
        LocalDateTime[] timeRange = getTimeRange(period);
        LocalDateTime startTime = timeRange[0];
        LocalDateTime endTime = timeRange[1];

        vo.setVisitorCount(chatHistoryMapper.countDistinctUsers(startTime, endTime));
        vo.setChatCount(chatHistoryMapper.countChats(startTime, endTime));

        // 2. 统计热门问题和景点（区分是否是全量总榜）
        if ("all".equals(period)) {
            // 总榜：查询所有历史累计数据，不考虑时间
            vo.setTopQuestions(hotQuestionMapper.selectTop10AllTime());
            vo.setTopScenics(hotScenicMapper.selectTopScenicsAllTime(5));
        } else {
            // 周期性统计：只查指定时间段
            LocalDate startDate = startTime.toLocalDate();
            LocalDate endDate = endTime.toLocalDate();
            vo.setTopQuestions(hotQuestionMapper.selectTop10ByDateRange(startDate, endDate));
            vo.setTopScenics(hotScenicMapper.selectTopScenicsByDateRange(startDate, endDate, 5));
        }

        return vo;
    }



    /**
     * 获取游客感受度报告
     * @param period 时间周期：day/week/month
     * @return
     */
    public VisitorReportVO getVisitorReport(String period) {
        LocalDateTime[] timeRange = getTimeRangeForReport(period);
        LocalDateTime startTime = timeRange[0];
        LocalDateTime endTime = timeRange[1];

        EmotionStatsVO emotionStats = calculateEmotionStats(startTime, endTime);

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        List<HotQuestionVO> topQuestions = hotQuestionMapper.selectTop10ByDateRange(startDate, endDate);
        List<HotScenicVO> topScenics = hotScenicMapper.selectTopScenicsByDateRange(startDate, endDate, 5);
        List<ComplaintStatsVO> complaintStats = complaintMapper.selectComplaintStats(startTime, endTime);

        // 不再调用 AI，直接返回基础数据
        return VisitorReportVO.builder()
                .emotionStats(emotionStats)
                .topQuestions(topQuestions)
                .topScenics(topScenics)
                .complaintStats(complaintStats)
                .build();
    }

    /**
     * 单独生成AI建议
     */
    public String getAiSuggestions(String period) {
        LocalDateTime[] timeRange = getTimeRangeForReport(period);
        LocalDateTime startTime = timeRange[0];
        LocalDateTime endTime = timeRange[1];

        EmotionStatsVO emotionStats = calculateEmotionStats(startTime, endTime);
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        List<HotQuestionVO> topQuestions = hotQuestionMapper.selectTop10ByDateRange(startDate, endDate);
        List<HotScenicVO> topScenics = hotScenicMapper.selectTopScenicsByDateRange(startDate, endDate, 5);
        List<ComplaintStatsVO> complaintStats = complaintMapper.selectComplaintStats(startTime, endTime);

        String context = buildAnalysisContext(emotionStats, topQuestions, topScenics, complaintStats);
        return analyzeAgent.generateManagementSuggestions(context);
    }



    /**
     * 根据传入时间段字符串，返回对应的开始时间和结束时间
     * @param period
     * @return
     */
    private LocalDateTime[] getTimeRange(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            // 今天：从 今天00:00:00 到 明天00:00:00
            case "today" -> new LocalDateTime[]{now.withHour(0).withMinute(0).withSecond(0), now.plusDays(1).withHour(0).withMinute(0).withSecond(0)};
            // 近7天：从 7天前的此刻 到 现在此刻
            case "week" -> new LocalDateTime[]{now.minusDays(7), now};
            // 近30天：从 30天前的此刻 到 现在此刻
            case "month" -> new LocalDateTime[]{now.minusDays(30), now};
            // 全部数据：从 2026-01-01 00:00 到 明天此刻
            case "all" -> new LocalDateTime[]{LocalDateTime.of(2026, 1, 1, 0, 0), now.plusDays(1)};
            // 默认情况：和今天一样
            default -> new LocalDateTime[]{now.withHour(0).withMinute(0).withSecond(0), now.plusDays(1).withHour(0).withMinute(0).withSecond(0)};
        };
    }

    private LocalDateTime[] getTimeRangeForReport(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "day" -> new LocalDateTime[]{now.withHour(0).withMinute(0).withSecond(0), now.plusDays(1).withHour(0).withMinute(0).withSecond(0)};
            case "week" -> new LocalDateTime[]{now.minusDays(7), now};
            case "month" -> new LocalDateTime[]{now.minusDays(30), now};
            default -> new LocalDateTime[]{now.withHour(0).withMinute(0).withSecond(0), now.plusDays(1).withHour(0).withMinute(0).withSecond(0)};
        };
    }

    /**
     * 根据 开始时间、结束时间 统计【满意/一般/不满】的情感数量
     * @param startTime
     * @param endTime
     * @return
     */
    private EmotionStatsVO calculateEmotionStats(LocalDateTime startTime, LocalDateTime endTime) {

        //1.调用Mapper，执行SQL查询：按情感类型分组统计数量
        List<Map<String, Object>> stats = chatHistoryMapper.selectEmotionStats(startTime, endTime);

        long satisfied = 0, normal = 0, dissatisfied = 0;

        // 2. 遍历查询出来的统计结果
        for (Map<String, Object> stat : stats) {
            String emotion = (String) stat.get("emotion");
            long count = ((Number) stat.get("count")).longValue();
            if ("满意".equals(emotion)) {
                satisfied = count;
            } else if ("一般".equals(emotion)) {
                normal = count;
            } else if ("不满".equals(emotion)) {
                dissatisfied = count;
            }
        }

        // 3. 计算评价总条数 = 满意 + 一般 + 不满
        long totalCount = satisfied + normal + dissatisfied;
        // 4.计算各类情感的占比（百分比），做了防除0判断（总数为0时百分比=0）
        //  满意百分比 = (满意数 / 总数) * 100
        double satisfiedPct = totalCount > 0 ? (satisfied * 100.0 / totalCount) : 0;
        double normalPct = totalCount > 0 ? (normal * 100.0 / totalCount) : 0;
        double dissatisfiedPct = totalCount > 0 ? (dissatisfied * 100.0 / totalCount) : 0;

        return EmotionStatsVO.builder()
                .satisfied(satisfied)
                .normal(normal)
                .dissatisfied(dissatisfied)
                .satisfiedPercentage(Math.round(satisfiedPct * 100.0) / 100.0)
                .normalPercentage(Math.round(normalPct * 100.0) / 100.0)
                .dissatisfiedPercentage(Math.round(dissatisfiedPct * 100.0) / 100.0)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 报告情况拼接
     * @param emotionStats
     * @param topQuestions
     * @param topScenics
     * @param complaintStats
     * @return
     */
    private String buildAnalysisContext(EmotionStatsVO emotionStats,
                                        List<HotQuestionVO> topQuestions,
                                        List<HotScenicVO> topScenics,
                                        List<ComplaintStatsVO> complaintStats) {
        StringBuilder sb = new StringBuilder();
        sb.append("【情感分布】\n");
        sb.append(String.format("满意: %d (%.2f%%)\n", emotionStats.getSatisfied(), emotionStats.getSatisfiedPercentage()));
        sb.append(String.format("一般: %d (%.2f%%)\n", emotionStats.getNormal(), emotionStats.getNormalPercentage()));
        sb.append(String.format("不满: %d (%.2f%%)\n\n", emotionStats.getDissatisfied(), emotionStats.getDissatisfiedPercentage()));

        sb.append("【高频问题 TOP5】\n");
        for (int i = 0; i < Math.min(5, topQuestions.size()); i++) {
            HotQuestionVO q = topQuestions.get(i);
            sb.append(String.format("%d. %s\n", i + 1, q.getQuestion()));
        }
        sb.append("\n");

        sb.append("【热门景点 TOP3】\n");
        for (int i = 0; i < Math.min(3, topScenics.size()); i++) {
            HotScenicVO s = topScenics.get(i);
            sb.append(String.format("%d. %s (%d次)\n", i + 1, s.getScenicName(), s.getCount()));
        }
        sb.append("\n");

        sb.append("【投诉类型分布】\n");
        for (ComplaintStatsVO c : complaintStats) {
            sb.append(String.format("%s: %d条 (待处理: %d, 处理中: %d)\n",
                    c.getComplaintType(), c.getCount(), c.getPendingCount(), c.getProcessingCount()));
        }

        sb.append("\n请基于以上数据，生成景区服务优化建议（200字以内，重点突出改进方向）：");
        return sb.toString();
    }
}



