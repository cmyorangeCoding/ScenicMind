package com.cmyhyq.pojo.dto;

import lombok.Data;

@Data
public class UserSearchDTO {

    private String phone;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String startTime;

    private String endTime;
}
