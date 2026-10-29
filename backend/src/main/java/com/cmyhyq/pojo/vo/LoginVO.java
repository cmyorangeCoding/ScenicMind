package com.cmyhyq.pojo.vo;

import lombok.Data;

@Data
public class LoginVO {

    private Long userId;

    private String token;

    //Token 过期时间戳
    private Long expireAt;

    private String nickname;

    private String avatarUrl;

}

