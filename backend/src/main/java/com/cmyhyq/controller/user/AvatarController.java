package com.cmyhyq.controller.user;



import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.vo.AvatarVO;
import com.cmyhyq.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/avatar")
@RequiredArgsConstructor
@Slf4j
public class AvatarController {

    private final AvatarService avatarService;

    @Value("${app.avatar.app-id}")
    private String appId;

    @Value("${app.avatar.scene-id}")
    private String sceneId;

    @Value("${app.avatar.api-key}")
    private String apiKey;

    @Value("${app.avatar.api-secret}")
    private String apiSecret;

    /**
     * 获取数字人关键配置
     * @return
     */
    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("apiKey",apiKey);
            config.put("apiSecret",apiSecret);
            config.put("appId", appId);
            config.put("sceneId", sceneId);
            log.info("获取数字人配置成功");
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取数字人配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取可用数字人列表
     * @return
     */
    @GetMapping("/list")
    public Result<List<AvatarVO>> listAvatars() {
        try {
            List<AvatarVO> list = avatarService.listEnabledForUser();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取数字人列表失败", e);
            return Result.error(e.getMessage());
        }
    }
}
