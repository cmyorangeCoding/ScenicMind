package com.cmyhyq;

import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DbConnectionTest {

    @Autowired
    private UserService userService;

    @Test
    void testInsertUser() {
        User user = new User();
        user.setNickname("test_user");
        userService.save(user);
        System.out.println("用户插入成功，ID：" + user.getUserId());
    }
}