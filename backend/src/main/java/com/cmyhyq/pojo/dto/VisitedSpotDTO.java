package com.cmyhyq.pojo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VisitedSpotDTO {
    private String spotName;           // 景点名称
    private LocalDateTime arrivalTime; // 到达时间
}
