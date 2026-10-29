package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_doc")
public class KnowledgeDoc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String category;
    private String tag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}