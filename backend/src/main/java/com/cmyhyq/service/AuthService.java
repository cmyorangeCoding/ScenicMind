package com.cmyhyq.service;

import com.cmyhyq.pojo.dto.LoginDTO;
import com.cmyhyq.pojo.vo.LoginVO;

public interface AuthService {

    /**
     * 用户登录
     * @param dto
     * @return
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出
     * @param token
     */
    void logout(String token);
}

