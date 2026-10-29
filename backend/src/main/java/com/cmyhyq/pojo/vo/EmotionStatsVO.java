package com.cmyhyq.pojo.vo;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionStatsVO {
    
    /**
     * 满意数量
     */
    private Long satisfied;
    
    /**
     * 一般数量
     */
    private Long normal;
    
    /**
     * 不满数量
     */
    private Long dissatisfied;
    
    /**
     * 满意百分比
     */
    private Double satisfiedPercentage;
    
    /**
     * 一般百分比
     */
    private Double normalPercentage;
    
    /**
     * 不满百分比
     */
    private Double dissatisfiedPercentage;
    
    /**
     * 总对话数（有情感标签的）
     */
    private Long totalCount;
}
