package com.cmyhyq.ai;

import com.cmyhyq.ai.agent.AnalyzeAgent;
import com.cmyhyq.ai.agent.ProfileAgent;
import com.cmyhyq.ai.agent.QaAgent;
import com.cmyhyq.ai.agent.RouteAgent;
import com.cmyhyq.ai.prompt.MasterPrompt;
import com.cmyhyq.ai.tool.ScenicTools;
import com.cmyhyq.common.RedisKeyConstants;
import com.cmyhyq.pojo.vo.HotQuestionVO;
import com.cmyhyq.service.AttractionService;
import com.cmyhyq.service.HotQuestionService;
import com.cmyhyq.service.WeatherService;
import com.cmyhyq.pojo.entity.ChatHistory;
import com.cmyhyq.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 主Agent - 统一处理用户请求，使用 Spring AI @Tool 注解进行智能路由
 * 
 * 核心设计理念：
 * - 使用 @Tool 注解将外部服务和子Agent能力封装为可调用工具
 * - LLM 根据用户问题自主决定调用哪个工具
 * - 使用实例绑定（Instance Binding）方案解决异步上下文丢失问题
 * 
 * 处理流程：
 * 1. 热门问答缓存命中 → 直接返回缓存并润色
 * 2. 图片问答 → 使用多模态模型处理
 * 3. 普通问答 → 使用 @Tool 注解的工具进行智能路由
 * 
 * 生命周期钩子（applyLifecycleHooks）：
 * - 落库：保存聊天记录到MySQL和Redis
 * - 画像更新：更新用户画像标签
 * - 情感分析：分析用户情感倾向
 * - ID回传：当检测到路线推荐时，返回chatId给前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterAgent {

    // ==================== 依赖注入 ====================
    private final ChatClient chatClient;          // Spring AI 聊天客户端
    private final QaAgent qaAgent;                // 知识问答Agent
    private final RouteAgent routeAgent;          // 路线规划Agent
    private final AnalyzeAgent analyzeAgent;      // 情感分析Agent
    private final ProfileAgent profileAgent;      // 用户画像Agent
    private final WeatherService weatherService;  // 天气服务
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatHistoryService chatHistoryService;
    private final AttractionService attractionService;
    private final HotQuestionService hotQuestionService;

    // ==================== 配置常量 ====================
    /**
     * 对话上下文记忆限制，只保留最近N轮
     */
    private static final int CONTEXT_LIMIT = 3;

    // ==================== 核心入口方法 ====================

    /**
     * 统一处理用户请求入口
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param question 用户问题
     * @param currentAttraction 当前景点
     * @param imageBase64 图片Base64（如果有）
     * @param imageUrl 图片URL（如果有）
     * @return 响应流
     */
    public Flux<String> handleRequest(Long userId, String sessionId, String question,
                                      String currentAttraction, String imageBase64, String imageUrl) {
        log.info("=== 开始处理用户请求 ===");
        log.info("用户ID: {}, 会话ID: {}, 用户问题: {}", userId, sessionId, question);

        // 获取对话上下文和当前时间
        String chatContext = getChatContext(userId, sessionId);
        String timeContext = getCurrentTimeHHmm();
        log.info("对话上下文长度: {}, 当前时间: {}", chatContext.length(), timeContext);

        // 指代词语义增强：将"它/这里/那"等指代词补充完整
        String enhancedQuestion = enhanceQuestionWithHistory(question, chatContext);
        if (!enhancedQuestion.equals(question)) {
            log.info("指代词语义增强: 原始问题 = '{}', 增强后 = '{}'", question, enhancedQuestion);
        }

        // ================= 分支1：热门问答缓存命中 =================
        HotQuestionVO cached = hotQuestionService.tryHitCache(question);
        if (cached != null) {
            log.info("命中热门问答缓存: 问题 = '{}'", cached.getQuestion());
            hotQuestionService.recordCacheHit(cached.getQuestion());
            Flux<String> flux = optimizeResponse(cached.getQuestion(), cached.getAnswer());
            return applyLifecycleHooks(userId, sessionId, question, flux, "缓存命中", chatContext, imageUrl);
        }

        // ================= 分支2：图片问答 =================
        // 保留原有的图片问答逻辑，因为多模态通常不走标准 Tool Calling
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            log.info("检测到图片，走图片问答分支，调用 qaAgent.answerWithImage()");
            Flux<String> flux = qaAgent.answerWithImage(question, imageBase64, currentAttraction, timeContext);
            return applyLifecycleHooks(userId, sessionId, question, flux, "图文问答", chatContext, imageUrl);
        }

        // ================= 分支3：@Tool 注解的智能路由分支=================
        log.info("走 Tool Calling 智能路由分支（实例绑定方案）");

        /**
         * 实例绑定方案核心实现：
         * - 为每个HTTP请求创建独立的 ScenicTools 实例
         * - 通过构造函数传递上下文参数（userId, chatContext, currentAttraction）
         * - 确保异步线程池中能正确获取上下文，避免 ThreadLocal 丢失问题
         */
        ScenicTools currentTools = new ScenicTools(
                weatherService,    // 天气服务依赖
                qaAgent,           // 知识问答Agent
                routeAgent,        // 路线规划Agent
                profileAgent,      // 用户画像Agent
                userId,            // 用户ID（上下文参数）
                chatContext,       // 对话上下文（上下文参数）
                currentAttraction  // 当前景点（上下文参数）
        );

        log.info("ScenicTools 实例已创建，准备调用 ChatClient...");
        log.info("注册的工具列表：getWeather, queryKnowledge, planRoute");

        /**
         * Spring AI Tool Calling 机制说明：
         * 1. LLM 根据系统提示词分析用户问题
         * 2. LLM 决定调用哪个工具（getWeather/queryKnowledge/planRoute）
         * 3. Spring AI 自动执行工具调用（ScenicTools 中的 @Tool 方法）
         * 4. LLM 对工具结果进行总结并返回最终回答
         * 
         * MasterPrompt 中已包含语言风格要求，LLM会按照要求润色回答
         * 
         * 日志说明：
         * - ScenicTools 中的每个工具方法都会打印详细的调用日志
         * - 搜索日志中的 "工具调用开始: xxx" 可以看到 LLM 调用了哪个工具
         * - 搜索日志中的 "工具调用参数" 可以看到传入的参数
         */
        Flux<String> responseFlux = chatClient.prompt()
            .system(MasterPrompt.TOOL_CALLING_SYSTEM_PROMPT)
            .user(u -> u.text(MasterPrompt.TOOL_CALLING_USER_PROMPT)
                    .param("time_context", timeContext)
                    .param("user_question", enhancedQuestion)
                    .param("currentAttraction", currentAttraction != null ? currentAttraction : "")
                    .param("chat_context", chatContext))
            .tools(currentTools)
            .stream()
            .content();

        log.info("Tool Calling 请求已发送，等待 LLM 决策...");
        log.info("请查看后续日志中的 '工具调用开始' 来确认 LLM 调用了哪个工具");
        return applyLifecycleHooks(userId, sessionId, question, responseFlux, "ToolCalling", chatContext, imageUrl);
    }

    // ==================== 生命周期钩子 ====================

    /**
     * 应用生命周期钩子 - 处理所有副作用（落库、画像更新、情感分析、ID回传）
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param question 用户问题
     * @param flux 响应流
     * @param intentType 意图类型
     * @param chatContext 对话上下文
     * @param imageUrl 图片URL
     * @return 处理后的响应流
     */
    private Flux<String> applyLifecycleHooks(Long userId, String sessionId, String question,
                                             Flux<String> flux, String intentType, String chatContext,
                                             String imageUrl) {
        StringBuilder fullAnswer = new StringBuilder();      // 收集完整回答内容
        AtomicReference<Long> chatIdRef = new AtomicReference<>();  // 存储chatId
        AtomicReference<Boolean> needsChatIdRef = new AtomicReference<>(false);  // 是否需要回传chatId

        // 1. 基础流：收集完整回答内容（带标记）并过滤给前端（不带标记）
        //  .doOnNext() - 每个元素到达时触发
        Flux<String> baseFlux = flux
                .doOnNext(fullAnswer::append)  // 内部收集带标记的内容
                .map(content -> {              // 给前端的内容过滤掉标记
                    String clean = content
                            .replace("[[ROUTE_RECOMMENDATION]]", "");
                    clean = clean.replaceAll("\\n+", "\n").replaceAll("^\\n|\\n$", "").trim();
                    return clean;
                });

        // 2. 钩子流：在流结束后执行所有副作用逻辑
        // 流结束时触发一次
        Flux<String> hookedFlux = baseFlux.doOnComplete(() -> {
            String finalAnswer = fullAnswer.toString();
            if (finalAnswer.isEmpty()) return;

            log.info("=== 流结束，开始处理生命周期钩子 ===");
            log.info("收集到的完整回答长度: {}", finalAnswer.length());

            // 检测是否有路线推荐标记
            boolean isRouteRecommendation = finalAnswer.contains("[[ROUTE_RECOMMENDATION]]");
            String cleanAnswer = finalAnswer
                    .replaceAll("\\[\\[ROUTE_RECOMMENDATION\\]\\]\\s*", "")
                    .replaceAll("\\n+", "\n")
                    .replaceAll("^\\n|\\n$", "")
                    .trim();
            log.info("是否检测到路线推荐标记: {}", isRouteRecommendation);
            log.info("清理后的回答: \"{}\"", cleanAnswer.substring(0, Math.min(200, cleanAnswer.length())));

            // 统一落库 + 更新Redis短时记忆和mysql长期记忆，获取chatId
            Long chatId = saveHistoryWithImage(userId, sessionId, question, cleanAnswer, imageUrl);
            chatIdRef.set(chatId);
            log.info("已保存聊天记录到MySQL和Redis，chatId = {}", chatId);

            // 标记是否需要回传chatId（用于前端收藏路线）
            if (isRouteRecommendation) {
                needsChatIdRef.set(true);
                log.info("检测到路线推荐标记，需要回传chatId给前端");
            }

            // 异步情感分析 + 热门问题统计（不阻塞主流程）
            log.info("开始异步情感分析和热门问题统计...");
            analyzeAgent.analyzeEmotion(chatId, question, cleanAnswer);
            analyzeAgent.recordHotTopics(question, cleanAnswer);
            log.info("情感分析和热门问题统计任务已启动...");

            // 画像更新判断：
            // - RouteAgent 内部已处理画像（同步）更新
            // - 其他场景（缓存命中、日常闲聊等）在此处处理
            if (!"RouteAgent".equals(intentType)) {
                log.info("更新用户画像，意图类型: {}", intentType);
                profileAgent.updateProfileAsync(userId, question, chatContext);
            } else {
                log.info("跳过画像更新，意图类型: {}", intentType);
            }
        });

        // 3. 智能ID回传：检测是否输出了路线推荐标记
        // 格式：[[CHAT_ID]]:chatId，与 WebSocket 约定的格式保持一致
        // concatWith() 会在主流完成后追加新元素
        return hookedFlux.concatWith(Flux.defer(() -> {
            Long id = chatIdRef.get();
            if (id != null && fullAnswer.toString().contains("[[ROUTE_RECOMMENDATION]]")) {
                // 前端会解析这个标记来获取 chatId，用于收藏路线功能
                return Flux.just("[[CHAT_ID]]:" + id);
            }
            return Flux.empty();
        }));
    }

    // ==================== 辅助方法 ====================

    /**
     * 保存历史记录 - 支持图片URL和Redis更新
     * 
     * 1. Redis短时记忆：用于上下文理解，保留最近5轮对话
     * 2. MySQL持久化：用于历史记录查询和数据分析
     * 
     * @return chatId 聊天记录ID
     */
    private Long saveHistoryWithImage(Long userId, String sessionId, String question, String answer, String imageUrl) {
        // 1. Redis短时记忆
        String key = RedisKeyConstants.aiMemoryKey(userId, sessionId);
        String record = String.format("User:%s%nAssistant:%s%n", question, answer);
        redisTemplate.opsForList().leftPush(key, record);
        redisTemplate.opsForList().trim(key, 0, CONTEXT_LIMIT - 1);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        log.debug("已更新Redis短时记忆，key: {}, 记录长度: {}", key, record.length());

        // 2. MySQL持久化
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setUserId(userId);
        chatHistory.setSessionId(sessionId);
        chatHistory.setQuestion(question);
        chatHistory.setAnswer(answer);
        chatHistory.setImageUrl(imageUrl);
        chatHistory.setCreateTime(LocalDateTime.now());
        chatHistoryService.save(chatHistory);
        log.debug("已保存聊天记录到MySQL，chatId: {}", chatHistory.getChatId());

        return chatHistory.getChatId();
    }

    /**
     * 增强问题 - 如果用户问题包含指代词且过短，尝试从历史记录中补充景点名
     * 
     * 处理场景：
     * - 用户问"它在哪里？" → 补充为"灵山大佛 它在哪里？"
     * - 用户问"这里什么时候关门？" → 补充为"灵山梵宫 这里什么时候关门？"
     */
    private String enhanceQuestionWithHistory(String question, String chatContext) {
        // 问题较长或不包含指代词，无需增强
        if (question.length() > 15 || (!question.contains("它") && !question.contains("这里") && 
            !question.contains("那") && !question.contains("什么时候") && !question.contains("怎么去"))) {
            return question;
        }

        // 从历史对话中查找景点名并补充
        String[] lines = chatContext.split("\n");
        for (String line : lines) {
            for (String name : attractionService.getAllAttractionNames()) {
                if (line.contains(name)) {
                    return name + " " + question;
                }
            }
        }
        return question;
    }

    /**
     * 获取当前时间，格式为HH:mm
     */
    private String getCurrentTimeHHmm() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 获取对话上下文 - 从Redis读取最近N轮
     */
    private String getChatContext(Long userId, String sessionId) {
        String key = RedisKeyConstants.aiMemoryKey(userId, sessionId);
        List<Object> history = redisTemplate.opsForList().range(key, 0, CONTEXT_LIMIT - 1);
        if (history == null || history.isEmpty()) {
            return "无历史对话上下文";
        }
        return history.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 缓存响应润色
     * 对缓存的回答进行润色，使其符合当前时间和语言风格要求
     */
    public Flux<String> optimizeResponse(String rawQuestion, String rawAnswer) {
        String timeContext = getCurrentTimeHHmm();
        log.info("调用 optimizeResponse 润色缓存响应，原始问题: {}", rawQuestion);
        return chatClient.prompt()
                .user(u -> u.text(MasterPrompt.RESPONSE_OPTIMIZATION_PROMPT)
                        .param("raw_question", rawQuestion)
                        .param("raw_answer", rawAnswer)
                        .param("time_context", timeContext))
                .stream()
                .content();
    }
}
