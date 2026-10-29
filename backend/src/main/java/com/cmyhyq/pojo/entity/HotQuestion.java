package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hot_question")
public class HotQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String standardQuestion;
    private String standardAnswer;
    private String keywords;
    private Integer count;
    private LocalDate statDate;

    // 新增字段
    private String sourceType; // knowledge_doc / attraction / manual
    private String sourceId;   // 对应知识ID或景点ID

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
