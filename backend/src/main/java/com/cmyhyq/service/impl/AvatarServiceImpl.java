package com.cmyhyq.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmyhyq.mapper.AvatarMapper;
import com.cmyhyq.pojo.entity.Avatar;
import com.cmyhyq.pojo.vo.AvatarVO;
import com.cmyhyq.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarServiceImpl implements AvatarService {

    private final AvatarMapper avatarMapper;

    /**
     * 管理端分页展示数字人
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     */
    public IPage<AvatarVO> pageForAdmin(Integer pageNum, Integer pageSize, String keyword) {
        Page<Avatar> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Avatar> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Avatar::getAvatarName, keyword)
                   .or()
                   .like(Avatar::getVcn, keyword);
        }
        
        wrapper.orderByDesc(Avatar::getCreateTime);
        
        IPage<Avatar> result = avatarMapper.selectPage(page, wrapper);
        
        return result.convert(this::toVO);
    }

    /**
     * 用户端获取可用数字人列表
     * @return
     */
    public List<AvatarVO> listEnabledForUser() {
        LambdaQueryWrapper<Avatar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Avatar::getStatus, 1)
               .orderByDesc(Avatar::getCreateTime);
        
        return avatarMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 添加数字人
     * @param avatar
     */
    public void addAvatar(Avatar avatar) {
        if (!StringUtils.hasText(avatar.getAvatarId())) {
            throw new IllegalArgumentException("形象ID不能为空");
        }
        if (!StringUtils.hasText(avatar.getVcn())) {
            throw new IllegalArgumentException("声音ID不能为空");
        }

        LambdaQueryWrapper<Avatar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Avatar::getAvatarId, avatar.getAvatarId());
        if (avatarMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("形象ID已存在");
        }

        avatar.setCreateTime(LocalDateTime.now());
        avatar.setUpdateTime(LocalDateTime.now());

        if (avatar.getStatus() == null) {
            avatar.setStatus(1);
        }
        if (avatar.getSpeed() == null) {
            avatar.setSpeed(50);
        }
        if (avatar.getPitch() == null) {
            avatar.setPitch(50);
        }
        if (avatar.getVolume() == null) {
            avatar.setVolume(50);
        }

        avatarMapper.insert(avatar);
    }

    /**
     * 更新数字人
     * @param avatar
     */
    public void updateAvatar(Avatar avatar) {
        if (!StringUtils.hasText(avatar.getAvatarId())) {
            throw new IllegalArgumentException("形象ID不能为空");
        }

        Avatar existing = avatarMapper.selectById(avatar.getAvatarId());
        if (existing == null) {
            throw new IllegalArgumentException("形象不存在");
        }

        avatar.setUpdateTime(LocalDateTime.now());
        avatarMapper.updateById(avatar);
    }

    /**
     * 删除数字人
     * @param avatarId
     */
    public void deleteAvatar(String avatarId) {
        if (!StringUtils.hasText(avatarId)) {
            throw new IllegalArgumentException("形象ID不能为空");
        }
        
        Avatar existing = avatarMapper.selectById(avatarId);
        if (existing == null) {
            throw new IllegalArgumentException("形象不存在");
        }
        
        avatarMapper.deleteById(avatarId);
    }

    /**
     * 启用/禁用数字人
     * @param avatarId
     * @param status
     */
    public void updateStatus(String avatarId, Integer status) {
        if (!StringUtils.hasText(avatarId)) {
            throw new IllegalArgumentException("形象ID不能为空");
        }
        
        Avatar existing = avatarMapper.selectById(avatarId);
        if (existing == null) {
            throw new IllegalArgumentException("形象不存在");
        }
        
        Avatar avatar = new Avatar();
        avatar.setAvatarId(avatarId);
        avatar.setStatus(status);
        avatar.setUpdateTime(LocalDateTime.now());
        
        avatarMapper.updateById(avatar);
    }

    private AvatarVO toVO(Avatar avatar) {
        AvatarVO vo = new AvatarVO();
        BeanUtils.copyProperties(avatar, vo);
        return vo;
    }
}
