package com.cmyhyq.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.pojo.vo.ChatHistoryDetailVO;
import com.cmyhyq.pojo.vo.UserDataVO;
import com.cmyhyq.service.ChatHistoryService;
import com.cmyhyq.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/user-manage")
@RequiredArgsConstructor
@Slf4j
public class UserManageController {

    private final UserService userService;
    private final ChatHistoryService chatHistoryService;

    /**
     * 根据手机号查询用户信息及会话历史（支持根据情绪和时间进行条件查询）
     * @param phone 手机号
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param emotion 情绪筛选（满意/一般/不满，可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 用户信息 + 分页的会话历史
     */
    @GetMapping("/detail")
    public Result<UserDataVO> getUserDetailByPhone(
            @RequestParam String phone,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String emotion,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            User user = userService.getUserByPhone(phone);
            if (user == null) {
                return Result.error("未找到该手机号对应的用户");
            }

            IPage<ChatHistoryDetailVO> chatHistoryPage = chatHistoryService.pageChatHistoryForAdmin(
                    user.getUserId(), pageNum, pageSize, emotion, startTime, endTime);

            UserDataVO vo = new UserDataVO();
            vo.setUserInfo(user);
            vo.setChatHistoryPage(chatHistoryPage);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询用户详情失败, phone={}", phone, e);
            return Result.error(e.getMessage());
        }
    }
}
