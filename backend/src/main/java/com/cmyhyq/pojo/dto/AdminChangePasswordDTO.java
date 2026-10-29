package com.cmyhyq.pojo.dto;

import lombok.Data;

@Data
public class AdminChangePasswordDTO {

    private Long adminId;

    private String oldPassword;

    private String newPassword;
}
