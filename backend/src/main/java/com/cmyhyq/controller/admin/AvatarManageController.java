package com.cmyhyq.controller.admin;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.util.OssService;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.AvatarDTO;
import com.cmyhyq.pojo.entity.Avatar;
import com.cmyhyq.pojo.vo.AvatarVO;
import com.cmyhyq.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/avatar")
@RequiredArgsConstructor
@Slf4j
public class AvatarManageController {

    private final AvatarService avatarService;
    private final OssService ossService;

    /**
     * 分页展示数字人
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<AvatarVO>> pageAvatar(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            IPage<AvatarVO> page = avatarService.pageForAdmin(pageNum, pageSize, keyword);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询数字人列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加数字人
     * @param dto
     * @return
     */
    @PostMapping
    public Result<String> addAvatar(@ModelAttribute AvatarDTO dto) {
        try {
            String imageUrl = ossService.uploadFile(dto.getAvatarImage());

            Avatar avatar = new Avatar();
            BeanUtils.copyProperties(dto, avatar);
            avatar.setAvatarImage(imageUrl);

            avatarService.addAvatar(avatar);
            return Result.success("新增成功");
        } catch (Exception e) {
            log.error("新增数字人失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新数字人
     * @param dto
     * @return
     */
    @PutMapping
    public Result<String> updateAvatar(@ModelAttribute AvatarDTO dto) {
        try {
            Avatar avatar = new Avatar();
            BeanUtils.copyProperties(dto, avatar);

            if (dto.getAvatarImage() != null && !dto.getAvatarImage().isEmpty()) {
                String imageUrl = ossService.uploadFile(dto.getAvatarImage());
                avatar.setAvatarImage(imageUrl);
            }

            avatarService.updateAvatar(avatar);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新数字人失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除数字人
     * @param avatarId
     * @return
     */
    @DeleteMapping("/{avatarId}")
    public Result<String> deleteAvatar(@PathVariable String avatarId) {
        try {
            avatarService.deleteAvatar(avatarId);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除数字人失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 禁用/启用数字人
     * @param avatarId
     * @param status
     * @return
     */
    @PutMapping("/{avatarId}/status")
    public Result<String> updateStatus(
            @PathVariable String avatarId,
            @RequestParam Integer status) {
        try {
            avatarService.updateStatus(avatarId, status);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            log.error("更新状态失败", e);
            return Result.error(e.getMessage());
        }
    }
}
