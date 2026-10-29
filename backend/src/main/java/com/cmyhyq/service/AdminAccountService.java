package com.cmyhyq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.dto.*;
import com.cmyhyq.pojo.entity.Admin;
import com.cmyhyq.pojo.vo.AdminLoginVO;
import com.cmyhyq.pojo.vo.AdminVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface AdminAccountService extends IService<Admin> {

    /**
     * 登录功能
     * @param dto
     * @return
     */
    AdminLoginVO login(AdminLoginDTO dto);

    /**
     * 登出
     * @param token
     */
    void logout(String token);

    /**
     * 分页查询
     * @param realName
     * @param pageNum
     * @param pageSize
     * @return
     */
    IPage<AdminVO> pageList(String realName, Integer pageNum, Integer pageSize);

    /**
     * 添加管理员
     * @param dto
     */
    void addAdmin(AdminAddDTO dto);

    /**
     * 编辑信息
     * @param dto
     */
    void updateAdmin(AdminUpdateDTO dto);

    /**
     * 启用/禁用账号
     * @param dto
     */
    void updateStatus(AdminStatusDTO dto);

    /**
     * 修改密码
     * @param dto
     */
    void changePassword(AdminChangePasswordDTO dto);
}
