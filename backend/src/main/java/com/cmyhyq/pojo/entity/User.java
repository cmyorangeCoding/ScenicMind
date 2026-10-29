package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "`user`")
public class User {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String nickname;

    private String avatarUrl;

    private String phone;


    private LocalDateTime lastLoginTime;


    /**
     * 存储标签权重的 JSON 字符串
     * 结构：[{"tag":"历史","weight":0.8,"lastUpdateTime":1714470000000}]
     */
    private String tagsJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
