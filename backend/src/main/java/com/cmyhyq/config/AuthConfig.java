package com.cmyhyq.config;

import com.cmyhyq.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {


    /**
     * 雪花算法生成器
     * @param workerId
     * @param datacenterId
     * @return
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${app.auth.worker-id:1}") long workerId,
            @Value("${app.auth.datacenter-id:1}") long datacenterId) {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }
}

