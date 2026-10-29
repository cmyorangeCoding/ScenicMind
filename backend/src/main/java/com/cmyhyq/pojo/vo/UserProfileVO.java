package com.cmyhyq.pojo.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long userId;
    private String nickname;
    private String phone;
    private String avatarUrl;
}
