package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.mapper.ChatHistoryMapper;
import com.cmyhyq.pojo.entity.ChatHistory;
import com.cmyhyq.pojo.vo.ChatHistoryDetailVO;
import com.cmyhyq.pojo.vo.ChatHistoryVO;
import com.cmyhyq.service.ChatHistoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    /**
     * 根据用户和会话 ID 查询历史记录
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return
     */
    public List<ChatHistoryVO> getHistoryBySession(Long userId, String sessionId) {
        // 1. 构建查询条件：匹配 userId 和 sessionId
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
               .eq(ChatHistory::getSessionId, sessionId)
               .orderByAsc(ChatHistory::getCreateTime); // 按时间正序排列

        // 2. 执行查询
        List<ChatHistory> records = this.list(wrapper);

        // 3. 转换为 VO 对象返回
        return records.stream().map(record -> {
            ChatHistoryVO vo = new ChatHistoryVO();
            BeanUtils.copyProperties(record, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 管理端：分页查询用户对话记录（支持情绪、时间筛选）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param emotion 情绪筛选（满意/一般/不满，可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return
     */
    public IPage<ChatHistoryDetailVO> pageChatHistoryForAdmin(Long userId, Integer pageNum, Integer pageSize, String emotion, String startTime, String endTime) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Page<ChatHistory> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatHistory::getUserId, userId);

        if (StringUtils.hasText(emotion)) {
            queryWrapper.eq(ChatHistory::getEmotion, emotion);
        }
        if (StringUtils.hasText(startTime)) {
            queryWrapper.ge(ChatHistory::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(ChatHistory::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        queryWrapper.orderByDesc(ChatHistory::getCreateTime);

        IPage<ChatHistory> chatPage = this.page(page, queryWrapper);

        IPage<ChatHistoryDetailVO> voPage = new Page<>(chatPage.getCurrent(), chatPage.getSize(), chatPage.getTotal());
        voPage.setRecords(chatPage.getRecords().stream().map(chat -> {
            ChatHistoryDetailVO vo = new ChatHistoryDetailVO();
            BeanUtils.copyProperties(chat, vo);
            return vo;
        }).toList());

        return voPage;
    }

    /**
     * 根据会话更新 imageUrl
     * @param userId
     * @param sessionId
     * @param question
     * @param imageUrl
     */
    public void updateImageUrlBySession(Long userId, String sessionId, String question, String imageUrl) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getSessionId, sessionId)
                .eq(ChatHistory::getQuestion, question)
                .orderByDesc(ChatHistory::getCreateTime)
                .last("LIMIT 1");

        ChatHistory history = this.getOne(wrapper);
        if (history != null) {
            history.setImageUrl(imageUrl);
            this.updateById(history);
        }
    }
}



