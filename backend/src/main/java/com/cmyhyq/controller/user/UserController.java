package com.cmyhyq.controller.user;

import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.UpdateUserDTO;
import com.cmyhyq.pojo.vo.UserProfileVO;
import com.cmyhyq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户个人信息
     * @param userId 用户ID
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getUserProfile(@RequestParam Long userId) {
        try {
            UserProfileVO profile = userService.getUserProfile(userId);
            return Result.success(profile);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改用户信息（支持只改昵称、只改手机或同时改）
     * @param dto 包含修改信息的 DTO
     */
    @PutMapping("/profile")
    public Result<String> updateUserProfile(@RequestBody UpdateUserDTO dto) {
        try {
            userService.updateUserProfile(dto);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
