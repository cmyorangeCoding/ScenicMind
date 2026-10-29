package com.cmyhyq.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminComplaintVO {

    private Long id;

    private Long userId;

    private String userPhone;

    private String complaintType;

    private String content;

    private Integer status;

    private String statusText;

    private String replyContent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
