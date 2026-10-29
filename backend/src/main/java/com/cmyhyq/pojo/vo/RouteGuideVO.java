package com.cmyhyq.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteGuideVO {
    private Long id;
    private String routeName;
    private String totalDuration;
    private LocalDateTime createTime;
    private List<String> attractions;
}

