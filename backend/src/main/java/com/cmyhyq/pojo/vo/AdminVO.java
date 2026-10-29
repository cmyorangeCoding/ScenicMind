package com.cmyhyq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminVO {

    private Long adminId;

    private String realName;

    private String account;

    private String phone;

    private Integer status;

    private String statusText;

    private LocalDateTime lastOperateTime;

    private LocalDateTime createTime;
}
