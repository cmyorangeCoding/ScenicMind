package com.cmyhyq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TravelNoteSummaryVO {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double durationHours;
    private LocalDateTime createTime;
}
