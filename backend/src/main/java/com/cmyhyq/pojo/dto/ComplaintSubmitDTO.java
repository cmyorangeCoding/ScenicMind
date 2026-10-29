package com.cmyhyq.pojo.dto;

import lombok.Data;

@Data
public class ComplaintSubmitDTO {

    private Long userId;

    //类型：服务态度/设施问题/环境卫生/app体验/其他
    private String complaintType;

    private String content;
}
