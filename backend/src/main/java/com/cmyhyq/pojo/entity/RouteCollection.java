package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("route_collection")
public class RouteCollection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long chatId;

    private String routeName;

    private String attractionsJson;

    private String totalDuration;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

