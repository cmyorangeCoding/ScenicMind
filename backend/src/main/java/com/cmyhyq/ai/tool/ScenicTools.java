package com.cmyhyq.ai.tool;

import com.cmyhyq.ai.agent.QaAgent;
import com.cmyhyq.ai.agent.RouteAgent;
import com.cmyhyq.ai.agent.ProfileAgent;
import com.cmyhyq.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 景区工具类 - 使用 @Tool 注解方式注册工具，供 Spring AI 的 Tool Calling 使用
 * 
 * 使用实例绑定（Instance Binding）方案解决异步上下文丢失问题：
 * - 不再作为 Spring 单例 Bean，改为每次请求创建独立实例
 * - 通过构造函数传递上下文参数（userId, chatContext, currentAttraction）
 * - 确保异步线程池中执行工具调用时能正确获取上下文
 * 
 * 注意：工具方法直接委托给子 Agent，不做额外处理，避免重复保存历史
 * Spring AI Tool Calling 机制说明：
 * 1. LLM 决定调用哪个工具（可以是一个或多个）
 * 2. Spring AI 自动执行工具调用
 * 3. 工具结果自动传给 LLM 进行总结
 * 4. 最终只返回一次总结结果给用户
 */
@Slf4j
public class ScenicTools {

    // ========== 依赖注入 ==========
    private final WeatherService weatherService;
    private final QaAgent qaAgent;
    private final RouteAgent routeAgent;
    private final ProfileAgent profileAgent;

    // ========== 实例绑定的上下文参数 ==========
    // 这些参数通过构造函数传入，与当前请求绑定，避免异步线程上下文丢失
    private final Long userId;
    private final String chatContext;
    private final String currentAttraction;

    /**
     * 构造函数 - 创建工具实例时传入所有依赖和上下文参数
     * 
     * @param weatherService 天气服务
     * @param qaAgent 知识问答 Agent
     * @param routeAgent 路线规划 Agent
     * @param profileAgent 用户画像 Agent
     * @param userId 当前用户ID（用于路线规划）
     * @param chatContext 对话上下文（用于指代消解和上下文理解）
     * @param currentAttraction 当前景点（用于上下文理解）
     */
    public ScenicTools(WeatherService weatherService, QaAgent qaAgent, 
                       RouteAgent routeAgent, ProfileAgent profileAgent,
                       Long userId, String chatContext, String currentAttraction) {
        // 依赖注入
        this.weatherService = weatherService;
        this.qaAgent = qaAgent;
        this.routeAgent = routeAgent;
        this.profileAgent = profileAgent;
        
        // 实例绑定的上下文参数
        this.userId = userId;
        this.chatContext = chatContext;
        this.currentAttraction = currentAttraction;
        
        log.info("ScenicTools 实例创建成功: userId={}, chatContext长度={}, currentAttraction={}",
                userId, chatContext != null ? chatContext.length() : 0, currentAttraction);
    }

    /**
     * 获取当前时间，格式为 HH:mm
     */
    private String getCurrentTimeHHmm() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }


    // ================= @Tool 注解的工具方法 =================

    /**
     * 工具1: 获取景区实时天气
     * 
     * @Tool 注解用于声明这是一个可供 LLM 调用的工具函数
     * - name: 工具名称，LLM 会使用这个名字来调用
     * - description: 工具描述，帮助 LLM 理解何时应该调用这个工具
     */
    @Tool(name = "getWeather", description = "查询灵山景区实时天气，包括温度、天气状况、湿度、空气质量等信息。如果用户问天气相关问题，应调用此工具。")
    public String getWeather() {
        log.info("============== 工具调用开始: getWeather ==============");
        log.info("LLM 调用了 getWeather 工具");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用 WeatherService 获取天气
            String weatherData = weatherService.getRealtimeWeather();
            
            long endTime = System.currentTimeMillis();
            log.info("WeatherService 调用成功，耗时: {}ms", (endTime - startTime));
            log.info("获取到的天气数据: {}", weatherData);
            log.info("============== 工具调用结束: getWeather ==============");
            
            return weatherData;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("WeatherService 调用失败，耗时: {}ms，错误: {}", (endTime - startTime), e.getMessage(), e);
            log.info("============== 工具调用结束: getWeather (失败) ==============");
            return "暂时无法查询到实时天气，建议您查看手机气象信息";
        }
    }

    /**
     * 工具2: 知识问答
     * 
     * 用于回答景区相关的知识性问题，包括景点历史、文化内涵、门票价格、演出时间等
     * 
     * @param question 用户问题
     * @return 问答响应流（直接委托给 QaAgent）
     */
    @Tool(name = "queryKnowledge", description = "回答景区知识问答，包括景点历史、文化内涵、门票价格、演出时间、餐饮住宿推荐等。如果用户询问景点信息、历史文化、票价等事实性问题，应调用此工具。")
    public String queryKnowledge(String question) {
        log.info("============== 工具调用开始: queryKnowledge ==============");
        log.info("LLM 调用了 queryKnowledge 工具，问题: '{}'", question);
        
        long startTime = System.currentTimeMillis();
        
        String ctx = this.chatContext != null ? this.chatContext : "";
        String attraction = this.currentAttraction != null ? this.currentAttraction : "";
        String timeContext = getCurrentTimeHHmm();
        
        log.info("工具调用参数(实例绑定): question='{}', chatContext长度={}, currentAttraction='{}', timeContext='{}'",
                question, ctx.length(), attraction, timeContext);
        
        try {
            Flux<String> answerFlux = qaAgent.answer(question, ctx, attraction, timeContext);
            String result = answerFlux.collect(Collectors.joining()).block();
            
            long endTime = System.currentTimeMillis();
            log.info("QaAgent 调用完成，耗时: {}ms，结果长度: {}", (endTime - startTime), result != null ? result.length() : 0);
            log.info("【工具返回内容】queryKnowledge 返回: {}", result != null && result.length() > 500 ? result.substring(0, 500) + "..." : (result != null ? result : "null"));
            log.info("============== 工具调用结束: queryKnowledge ==============");
            
            return result != null ? result : "抱歉，我暂时无法回答这个问题";
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("QaAgent 调用失败，耗时: {}ms，错误: {}", (endTime - startTime), e.getMessage(), e);
            log.info("============== 工具调用结束: queryKnowledge (失败) ==============");
            return "抱歉，我暂时无法回答这个问题";
        }
    }

    /**
     * 工具3: 路线规划
     * 
     * 用于路线规划与导航，包括问路、找出口、附近推荐、完整路线规划等
     * 
     * @param question 用户问题
     * @return 路线规划响应流（直接委托给 RouteAgent）
     */
    @Tool(name = "planRoute", description = "路线规划与导航，包括问路、找出口、附近景点推荐、完整路线规划等。如果用户问路、规划游览路线、找某个地点等，应调用此工具。")
    public String planRoute(String question) {
        log.info("============== 工具调用开始: planRoute ==============");
        log.info("LLM 调用了 planRoute 工具，问题: '{}'", question);
        
        long startTime = System.currentTimeMillis();
        
        Long uid = this.userId;
        String ctx = this.chatContext != null ? this.chatContext : "";
        String attraction = this.currentAttraction != null ? this.currentAttraction : "";
        String timeContext = getCurrentTimeHHmm();
        
        log.info("工具调用参数(实例绑定): question='{}', userId={}, chatContext长度={}, currentAttraction='{}', timeContext='{}'",
                question, uid, ctx.length(), attraction, timeContext);
        
        if (uid == null) {
            log.warn("userId 为 null，不能进行路线规划");
            log.info("============== 工具调用结束: planRoute (未登录) ==============");
            return "抱歉，需要先登录才能进行路线规划哦~";
        }
        
        try {
            log.info("先调用 ProfileAgent 更新用户画像...");
            String userProfile = profileAgent.updateAndGetProfile(uid, question, ctx);
            log.info("ProfileAgent 调用成功，用户画像长度: {}", userProfile.length());
            
            Flux<String> answerFlux = routeAgent.planRoute(uid, question, userProfile, attraction, ctx, timeContext);
            String result = answerFlux.collect(Collectors.joining()).block();
            
            long endTime = System.currentTimeMillis();
            log.info("RouteAgent 调用完成，耗时: {}ms，结果长度: {}", (endTime - startTime), result != null ? result.length() : 0);
            log.info("【工具返回内容】planRoute 返回: {}", result != null && result.length() > 500 ? result.substring(0, 500) + "..." : (result != null ? result : "null"));
            log.info("============== 工具调用结束: planRoute ==============");
            
            return result != null ? result : "抱歉，我暂时无法帮您规划路线";
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("RouteAgent 调用失败，耗时: {}ms，错误: {}", (endTime - startTime), e.getMessage(), e);
            log.info("============== 工具调用结束: planRoute (失败) ==============");
            return "抱歉，我暂时无法帮您规划路线";
        }
    }
}
