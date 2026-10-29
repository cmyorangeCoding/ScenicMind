package com.cmyhyq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.dto.UpdateUserDTO;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.pojo.vo.UserProfileVO;

public interface UserService extends IService<User> {

    /**
     * 查看个人信息
     * @param userId
     * @return
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 修改个人信息
     * @param dto
     */
    void updateUserProfile(UpdateUserDTO dto);

    /**
     * 管理端：根据手机号查询用户
     * @param phone 手机号
     * @return 用户详情（含标签）
     */
    User getUserByPhone(String phone);
}