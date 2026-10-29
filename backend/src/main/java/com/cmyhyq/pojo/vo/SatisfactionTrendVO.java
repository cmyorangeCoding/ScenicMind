package com.cmyhyq.pojo.vo;



import lombok.Data;
import java.time.LocalDate;

@Data
public class SatisfactionTrendVO {
    
    /**
     * 统计日期
     */
    private LocalDate statDate;
    
    /**
     * 平均满意度分数（1-3分）
     */
    private Double avgScore;
    
    /**
     * 对话总数
     */
    private Integer chatCount;
}
