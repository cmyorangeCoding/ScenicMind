package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long adminId;

    private String realName;

    private String account;

    private String password;

    private String phone;

    private Integer status;

    private LocalDateTime lastOperateTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
