package com.cmyhyq.pojo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TravelNoteGenerateDTO {
    private Long userId;
    private List<VisitedSpotDTO> visitedSpots;  // 游览景点列表
    private LocalDateTime startTime;            // 开始时间
    private LocalDateTime endTime;              // 结束时间
}
