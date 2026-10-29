package com.cmyhyq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatHistoryVO {
    private Long chatId;
    private String question;
    private String answer;
    private String imageUrl;
    private LocalDateTime createTime;
}
