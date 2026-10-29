package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_note")
public class TravelNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double durationHours;

    private String visitedSpotsJson;

    private LocalDateTime createTime;
    
    /**
     * 状态：PENDING-生成中，COMPLETED-已完成，FAILED-失败
     */
    private String status;

}
