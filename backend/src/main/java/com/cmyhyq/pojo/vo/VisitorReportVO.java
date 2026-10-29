package com.cmyhyq.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorReportVO {
    
    /**
     * 情感统计数据
     */
    private EmotionStatsVO emotionStats;
    
    /**
     * 热门问题 TOP10
     */
    private List<HotQuestionVO> topQuestions;
    
    /**
     * 热门景点 TOP5
     */
    private List<HotScenicVO> topScenics;
    
    /**
     * 投诉统计数据
     */
    private List<ComplaintStatsVO> complaintStats;

}
