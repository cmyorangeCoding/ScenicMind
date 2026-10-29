package com.cmyhyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmyhyq.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}