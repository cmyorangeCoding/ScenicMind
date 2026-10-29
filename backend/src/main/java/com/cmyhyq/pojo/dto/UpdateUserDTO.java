package com.cmyhyq.pojo.dto;

import lombok.Data;

@Data
public class UpdateUserDTO {
    private Long userId;      // 必须传，标识用户
    private String nickname;  // 用户名（可选，不传则不修改）
    private String phone;     // 手机号（可选，不传则不修改）
}
