package com.cmyhyq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TravelNoteVO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double durationHours;
    private List<VisitedSpotVO> visitedSpots;
    private LocalDateTime createTime;
}
