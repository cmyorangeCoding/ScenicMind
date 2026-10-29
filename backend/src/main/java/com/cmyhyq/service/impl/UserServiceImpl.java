package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.mapper.UserMapper;
import com.cmyhyq.pojo.dto.UpdateUserDTO;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.pojo.vo.UserProfileVO;
import com.cmyhyq.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 查看个人信息
     * @param userId
     * @return
     */
    public UserProfileVO getUserProfile(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    /**
     * 修改个人信息
     * @param dto
     */
    @Override
    public void updateUserProfile(UpdateUserDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        // 使用 LambdaUpdateWrapper 实现"部分更新"
        // 这样如果前端没传某个字段，数据库里的该字段就不会被修改
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, dto.getUserId());

        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            updateWrapper.set(User::getNickname, dto.getNickname());
        }

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            // 这里可以加手机号格式校验，或者检查手机号是否已存在
            updateWrapper.set(User::getPhone, dto.getPhone());
        }

        // 如果两个都没传，就不执行更新
        if (updateWrapper.getSqlSet() != null) {
            this.update(updateWrapper);
        }
    }

    /**
     * 根据手机号查询用户信息
     * @param phone 手机号
     * @return
     */
    public User getUserByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        return this.getOne(queryWrapper);
    }


}