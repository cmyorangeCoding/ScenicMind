package com.cmyhyq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocVO {

    private Long id;

    private String title;

    private String content;

    private String category;

    private String tag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
