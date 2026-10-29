package com.cmyhyq.controller.user;


import com.cmyhyq.ai.MasterAgent;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.vo.ChatHistoryVO;
import com.cmyhyq.pojo.vo.HotQuestionVO;
import com.cmyhyq.util.AliyunAuthService;
import com.cmyhyq.service.ChatHistoryService;
import com.cmyhyq.service.HotQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"user/ai", "user/aichat"})
@RequiredArgsConstructor
@Slf4j
public class AIChatController {

    private final MasterAgent masterAgent;
    private final ChatHistoryService chatHistoryService;
    private final HotQuestionService hotQuestionService;
    private final AliyunAuthService aliyunAuthService;



    /**
     * AI对话功能（已弃用，用户端AI对话走 websocket）
     *
     * @param userId
     * @param sessionId
     * @param message
     * @param currentAttraction
     * @return
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam Long userId,
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) String currentAttraction,
            @RequestParam(required = false) MultipartFile image) {

        String imageUrl = null;
        String imageBase64 = null;

        // 1. 如果有图片，先传 OSS 并转 Base64
        if (image != null && !image.isEmpty()) {
            try {
                // 核心：使用改造后的 uploadFile 方法，支持 MultipartFile 直接传入
                imageBase64 = Base64.getEncoder().encodeToString(image.getBytes());
            } catch (Exception e) {
                return Flux.just("图片上传失败");
            }
        }

        // 2. 统一交给 MasterAgent 处理（传入图片信息）
        return masterAgent.handleRequest(userId, sessionId, message, currentAttraction, imageBase64, imageUrl);
    }


    /**
     * 查询历史对话记录
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 历史对话列表
     */
    @GetMapping("/chatHistory")
    public Result<List<ChatHistoryVO>> getChatHistory(
            @RequestParam Long userId,
            @RequestParam String sessionId) {
        try {
            List<ChatHistoryVO> history = chatHistoryService.getHistoryBySession(userId, sessionId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error("查询历史对话失败");
        }
    }


    /**
     * 获取热门问答 Top 10
     */
    @GetMapping("/hot-questions")
    public Result<List<HotQuestionVO>> getHotQuestions() {
        try {
            List<HotQuestionVO> list = hotQuestionService.getTopHotQuestions();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询热门问答失败");
        }
    }

    /**
     * 获取阿里云智能语音 Token
     * 前端使用此 Token 进行语音识别
     */
    @GetMapping("/getAliyunToken")
    public Result<Map<String, Object>> getAliyunToken() {
        try {
            String token = aliyunAuthService.getAccessToken();
            long expireTime = aliyunAuthService.getTokenExpireTime();
            String appKey= aliyunAuthService.getNlsAppKey();

            // 返回给前端的信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("expireTime", expireTime);
            data.put("appKey", appKey);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取语音 Token 失败");
        }
    }
}
