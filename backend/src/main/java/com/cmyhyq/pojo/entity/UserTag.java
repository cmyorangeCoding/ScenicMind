package com.cmyhyq.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTag {
    private String tag;          // 标签名
    private double weight;       // 权重
    private long lastUpdateTime; // 时间戳（毫秒）
}
