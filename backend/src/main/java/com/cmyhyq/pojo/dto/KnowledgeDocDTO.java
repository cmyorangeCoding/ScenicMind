package com.cmyhyq.pojo.dto;

import lombok.Data;

@Data
public class KnowledgeDocDTO {

    private Long id;

    private String title;

    private String content;

    private String category;

    private String tag;
}
