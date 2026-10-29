package com.cmyhyq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VisitedSpotVO {
    private String spotName;
    private LocalDateTime arrivalTime;
}
