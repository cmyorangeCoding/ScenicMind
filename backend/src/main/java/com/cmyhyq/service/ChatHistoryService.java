package com.cmyhyq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.entity.ChatHistory;
import com.cmyhyq.pojo.vo.ChatHistoryDetailVO;
import com.cmyhyq.pojo.vo.ChatHistoryVO;

import java.util.List;

public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 根据用户和会话 ID 查询历史记录
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 历史对话列表
     */
    List<ChatHistoryVO> getHistoryBySession(Long userId, String sessionId);


    /**
     * 管理端：分页查询用户对话记录（支持情绪、时间筛选）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param emotion 情绪筛选（满意/一般/不满，可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 对话记录分页
     */
    IPage<ChatHistoryDetailVO> pageChatHistoryForAdmin(Long userId, Integer pageNum, Integer pageSize, String emotion, String startTime, String endTime);

    /**
     * 根据会话更新 imageUrl
     * @param userId
     * @param sessionId
     * @param question
     * @param imageUrl
     */
    void updateImageUrlBySession(Long userId, String sessionId, String question, String imageUrl);

}
