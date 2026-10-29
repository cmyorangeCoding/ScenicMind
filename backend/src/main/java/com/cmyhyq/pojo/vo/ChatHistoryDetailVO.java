package com.cmyhyq.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatHistoryDetailVO {

    private Long chatId;

    private Long userId;

    private String sessionId;

    private String question;

    private String answer;

    private String emotion;

    private LocalDateTime createTime;
}
