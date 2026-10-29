package com.cmyhyq.pojo.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.pojo.entity.User;
import lombok.Data;

@Data
public class UserDataVO {

    private User userInfo;

    private IPage<ChatHistoryDetailVO> chatHistoryPage;
}
