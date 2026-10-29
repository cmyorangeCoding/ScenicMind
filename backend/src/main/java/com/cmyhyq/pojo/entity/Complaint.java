package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("complaint")
public class Complaint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String complaintType;

    private String content;


    private Integer status;

    private String replyContent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
