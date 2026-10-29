package com.cmyhyq.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.*;
import com.cmyhyq.pojo.vo.AdminLoginVO;
import com.cmyhyq.pojo.vo.AdminVO;
import com.cmyhyq.service.AdminAccountService;
import com.cmyhyq.config.AdminAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    /**
     * 管理员登录
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody AdminLoginDTO dto) {
        try {
            return Result.success(adminAccountService.login(dto));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 登出
     * @param token
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            adminAccountService.logout(token);
            return Result.success("登出成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询
     * @param realName
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<AdminVO>> pageList(
            @RequestParam(required = false) String realName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
            ) {
        try {
            return Result.success(adminAccountService.pageList(realName, pageNum, pageSize));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    /**
     * 添加管理员
     * @param dto
     * @return
     */
    @PostMapping("/add")
    public Result<String> addAdmin(@RequestBody AdminAddDTO dto) {
        try {
            adminAccountService.addAdmin(dto);
            return Result.success("添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改管理员信息
     * @param dto
     * @return
     */
    @PutMapping("/update")
    public Result<String> updateAdmin(@RequestBody AdminUpdateDTO dto) {
        try {
            adminAccountService.updateAdmin(dto);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用账号
     * @param dto
     * @return
     */
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestBody AdminStatusDTO dto) {
        try {
            adminAccountService.updateStatus(dto);
            return Result.success("状态修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     * @param dto
     * @param request
     * @return
     */
    @PutMapping("/password")
    public Result<String> changePassword(@RequestBody AdminChangePasswordDTO dto,
                                         HttpServletRequest request) {
        try {
            String adminIdStr = (String) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_ID);
            if (adminIdStr == null) {
                return Result.error("未登录");
            }
            dto.setAdminId(Long.parseLong(adminIdStr));
            adminAccountService.changePassword(dto);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
