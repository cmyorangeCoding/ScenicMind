package com.cmyhyq.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatsVO {
    
    /**
     * 投诉类型
     */
    private String complaintType;
    
    /**
     * 投诉数量
     */
    private Long count;
    
    /**
     * 待处理数量
     */
    private Long pendingCount;
    
    /**
     * 处理中数量
     */
    private Long processingCount;
}
