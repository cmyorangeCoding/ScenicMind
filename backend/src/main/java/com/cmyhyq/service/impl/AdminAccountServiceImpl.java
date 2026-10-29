package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.common.RedisKeyConstants;
import com.cmyhyq.mapper.AdminMapper;
import com.cmyhyq.pojo.dto.*;
import com.cmyhyq.pojo.entity.Admin;
import com.cmyhyq.pojo.vo.AdminLoginVO;
import com.cmyhyq.pojo.vo.AdminVO;
import com.cmyhyq.service.AdminAccountService;
import com.cmyhyq.util.TokenUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminAccountService {

    @Value("${app.auth.admin-token-secret}")
    private String adminTokenSecret;

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录功能
     * @param dto
     * @return
     */
    public AdminLoginVO login(AdminLoginDTO dto) {
        if (dto.getAccount() == null || dto.getAccount().isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getAccount, dto.getAccount());
        Admin admin = this.getOne(queryWrapper);


        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        String token = TokenUtil.generateToken(admin.getAdminId(), System.currentTimeMillis() + 86400 * 1000L, adminTokenSecret);

        admin.setLastOperateTime(LocalDateTime.now());
        this.updateById(admin);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setAdminId(admin.getAdminId());
        vo.setRealName(admin.getRealName());
        vo.setAccount(admin.getAccount());
        vo.setToken(token);

        return vo;
    }


    /**
     * 登出
     * @param token
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token 不能为空");
        }

        try {
            Claims claims = TokenUtil.verifyAndParse(token, adminTokenSecret);
            if (claims == null) {
                return;
            }

            long expirationTime = claims.getExpiration().getTime();
            long currentTime = System.currentTimeMillis();
            long remainingTimeSeconds = (expirationTime - currentTime) / 1000;

            if (remainingTimeSeconds <= 0) {
                return;
            }

            String blacklistKey = RedisKeyConstants.tokenBlacklistKey(token);
            stringRedisTemplate.opsForValue().set(blacklistKey, "blacklisted", remainingTimeSeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("登出失败，请稍后重试");
        }
    }

    /**
     * 分页查询
     * @param realName
     * @param pageNum
     * @param pageSize
     * @return
     */
    public IPage<AdminVO> pageList(String realName, Integer pageNum, Integer pageSize) {
        Page<Admin> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        if (realName != null && !realName.isBlank()) {
            queryWrapper.like(Admin::getRealName, realName);
        }
        queryWrapper.orderByDesc(Admin::getCreateTime);

        IPage<Admin> adminPage = this.page(page, queryWrapper);

        IPage<AdminVO> voPage = new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
        voPage.setRecords(adminPage.getRecords().stream().map(admin -> {
            AdminVO vo = new AdminVO();
            BeanUtils.copyProperties(admin, vo);
            vo.setStatusText(admin.getStatus() != null && admin.getStatus() == 1 ? "启用" : "禁用");
            return vo;
        }).toList());

        return voPage;
    }

    /**
     * 新增管理员
     * @param dto
     */
    public void addAdmin(AdminAddDTO dto) {
        if (dto.getAccount() == null || dto.getAccount().isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (dto.getRealName() == null || dto.getRealName().isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getAccount, dto.getAccount());
        if (this.count(queryWrapper) > 0) {
            throw new IllegalArgumentException("账号已存在");
        }

        Admin admin = new Admin();
        admin.setRealName(dto.getRealName());
        admin.setAccount(dto.getAccount());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setPhone(dto.getPhone());
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        this.save(admin);
    }

    /**
     * 编辑信息
     * @param dto
     */
    public void updateAdmin(AdminUpdateDTO dto) {
        if (dto.getAdminId() == null) {
            throw new IllegalArgumentException("管理员ID不能为空");
        }

        Admin admin = this.getById(dto.getAdminId());
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }

        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            admin.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            admin.setPhone(dto.getPhone());
        }

        admin.setUpdateTime(LocalDateTime.now());
        this.updateById(admin);
    }

    /**
     * 启用/禁用账号
     * @param dto
     */
    public void updateStatus(AdminStatusDTO dto) {
        if (dto.getAdminId() == null) {
            throw new IllegalArgumentException("管理员ID不能为空");
        }

        Admin admin = this.getById(dto.getAdminId());
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }

        admin.setStatus(dto.getStatus());
        admin.setUpdateTime(LocalDateTime.now());
        this.updateById(admin);
    }

    /**
     * 修改密码
     * @param dto
     */
    public void changePassword(AdminChangePasswordDTO dto) {
        if (dto.getAdminId() == null) {
            throw new IllegalArgumentException("管理员ID不能为空");
        }
        if (dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
            throw new IllegalArgumentException("旧密码不能为空");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("新密码不能为空");
        }

        Admin admin = this.getById(dto.getAdminId());
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        admin.setUpdateTime(LocalDateTime.now());
        this.updateById(admin);
    }

}
