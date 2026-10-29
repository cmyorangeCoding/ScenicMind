package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hot_scenic")
public class HotScenic {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String scenicId;
    private String scenicName;
    private Integer count;
    private LocalDate statDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
