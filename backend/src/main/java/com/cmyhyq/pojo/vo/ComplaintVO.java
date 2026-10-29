package com.cmyhyq.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintVO {
    private Long id;
    private String complaintType;
    private String content;
    private Integer status;
    private String replyContent;
    private LocalDateTime createTime;
}
