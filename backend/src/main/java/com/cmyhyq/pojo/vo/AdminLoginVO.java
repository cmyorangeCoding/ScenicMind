package com.cmyhyq.pojo.vo;

import lombok.Data;

@Data
public class AdminLoginVO {

    private Long adminId;

    private String realName;

    private String account;

    private String token;
}
