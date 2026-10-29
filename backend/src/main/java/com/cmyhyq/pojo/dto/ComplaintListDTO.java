package com.cmyhyq.pojo.dto;

import lombok.Data;

@Data
public class ComplaintListDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Integer status;

    private String startTime;

    private String endTime;
}
