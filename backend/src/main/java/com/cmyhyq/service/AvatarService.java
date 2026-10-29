package com.cmyhyq.service;



import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.pojo.entity.Avatar;
import com.cmyhyq.pojo.vo.AvatarVO;

import java.util.List;

public interface AvatarService {

    /**
     * 管理端分页展示数字人
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     */
    IPage<AvatarVO> pageForAdmin(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 用户端获取可用数字人列表
     * @return
     */
    List<AvatarVO> listEnabledForUser();

    /**
     * 添加数字人
     * @param avatar
     */
    void addAvatar(Avatar avatar);

    /**
     * 更新数字人
     * @param avatar
     */
    void updateAvatar(Avatar avatar);

    /**
     * 删除数字人
     * @param avatarId
     */
    void deleteAvatar(String avatarId);

    /**
     * 启用/禁用数字人
     * @param avatarId
     * @param status
     */
    void updateStatus(String avatarId, Integer status);
}
