package com.cmyhyq.ai.agent;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cmyhyq.ai.prompt.QaPrompt;
import com.cmyhyq.ai.rag.VectorStoreQueryService;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.*;

import static com.alibaba.dashscope.utils.Constants.apiKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class QaAgent {

    @Value("${app.dashscope.api-key}")
    private String apiKey;

    private final ChatClient chatClient;
    private final VectorStoreQueryService vectorStoreQueryService;
    private final AttractionService attractionService;
    private final MultiModalConversation multiModalConversation;

    /**
     * 原有纯文本回答逻辑
     */
    public Flux<String> answer(String question, String context, String currentAttraction, String timeContext) {
        try {
            // 1. 调用 mysql+RAG 知识库构建逻辑
            String finalKnowledgeContext = buildKnowledgeContext(question, currentAttraction);
            if (finalKnowledgeContext.isEmpty()) {
                return Flux.just("抱歉呀，关于这个问题我暂时没有相关介绍，你可以问问景区其他景点或路线哦~");
            }

            // 2. 大模型流式输出
            return chatClient.prompt()
                    .user(u -> u.text(QaPrompt.QA_PROMPT)
                            .param("rag_context", finalKnowledgeContext)
                            .param("user_question", question)
                            .param("chat_context", context)
                            .param("current_location", currentAttraction != null ? currentAttraction : "未知")
                            .param("time_context", timeContext))
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("QaAgent 处理异常", e);
            return Flux.just("抱歉呀，服务暂时有点小问题，请稍后再试~");
        }
    }


    /**
     * 处理图文混合请求（原生流式输出版）
     */
    public Flux<String> answerWithImage(String question, String imageBase64, String currentAttraction, String timeContext) {
        return Flux.<String>create(sink -> {
            try {
                // 1. 全量景点资料库构建逻辑
                List<Attraction> allAttractions = attractionService.list();
                if (allAttractions.isEmpty()) {
                    sink.next("抱歉呀，系统里还没有录入景点信息呢~");
                    sink.complete();
                    return;
                }

                StringBuilder knowledgeBase = new StringBuilder("【景点资料库（请严格基于此信息进行视觉匹配）】:\n");
                for (Attraction attr : allAttractions) {
                    knowledgeBase.append(String.format("""
                        ---
                        名称: %s
                        特征/亮点: %s
                        建筑参数: %s
                        文化内涵: %s
                        详细介绍: %s
                        演艺/开放信息
                        ---
                        """,
                            attr.getAttrName(), attr.getHighlights(),
                            attr.getBuildingInfo(), attr.getCulture(), attr.getDetail(),attr.getPerformanceInfo()));
                }

                // 2. 构建 Prompt
                String prompt = QaPrompt.MULTIMODAL_QA_PROMPT
                        .replace("{rag_context}", knowledgeBase.toString())
                        .replace("{user_question}", question)
                        .replace("{current_location}", currentAttraction != null ? currentAttraction : "未知")
                        .replace("{time_context}", timeContext);

                // 3. 构建消息
                String imageUri = imageBase64.startsWith("data:") ? imageBase64 : "data:image/jpeg;base64," + imageBase64;
                Map<String, Object> imageContent = Map.of("image", imageUri);

                List<MultiModalMessage> messages = Collections.singletonList(
                        MultiModalMessage.builder()
                                .role(Role.USER.getValue())
                                .content(Arrays.asList(imageContent, Map.of("text", prompt)))
                                .build()
                );

                // 4. 构建参数
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .apiKey(apiKey)
                        .model("qwen3-vl-plus")
                        .messages(messages)
                        .build();

                // 5. 【核心】执行流式调用
                // 注意：streamCall 内部会处理异步，我们只需要把结果塞进 sink
                multiModalConversation.streamCall(param, new ResultCallback<MultiModalConversationResult>() {
                    @Override
                    public void onEvent(MultiModalConversationResult result) {
                        String content = result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString();
                        if (content != null && !content.isEmpty()) {
                            sink.next(content);
                        }
                    }

                    @Override
                    public void onComplete() {
                        log.info("图文问答流式输出完成");
                        sink.complete();
                    }

                    @Override
                    public void onError(Exception e) {
                        // 【关键修复】阿里云的 response_error 是“假异常”，字其实已经传完了。
                        // 如果这里调用 sink.error()，会导致 MasterAgent 的 doOnComplete（负责存记忆）被跳过！
                        // 必须转为 sink.complete()，才能让主流程正常执行入库逻辑。
                        if (e.getMessage() != null && e.getMessage().contains("response_error")) {
                            log.warn("捕获到阿里云流式结束假异常，已转为正常结束以触发记忆存储");
                            sink.complete();
                        } else {
                            log.error("图文问答流式输出异常", e);
                            sink.error(e);
                        }
                    }
                });

            } catch (Exception e) {
                log.error("图文问答启动失败", e);
                sink.error(e);
            }
                })
                .subscribeOn(Schedulers.boundedElastic()); // 在这里指定后台线程运行
    }




    /**
     * 文本问答核心逻辑抽取：构建知识库上下文（MySQL + RAG）
     */
    private String buildKnowledgeContext(String question, String currentAttraction) {
        StringBuilder combinedContext = new StringBuilder();

        // 1. 提取景点名称
        String targetAttraction = extractAttractionName(question, currentAttraction);

        // 2. 查询 MySQL 景点
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        if (targetAttraction != null) {
            queryWrapper.eq("attr_name", targetAttraction);
        }

        List<Attraction> attractions = Collections.emptyList();
        if (targetAttraction != null) {
            attractions = attractionService.list(queryWrapper);
            log.info("MySQL 成功匹配到景点: {}", targetAttraction);
        }

        if (!attractions.isEmpty()) {
            combinedContext.append("【核心景点详情（来自 MySQL）】:\n");
            attractions.forEach(attr -> {
                combinedContext.append(String.format("""
                    景点名称: %s
                    景点类型: %s
                    具体位置: %s
                    游玩亮点: %s
                    建筑/景观参数: %s
                    核心功能: %s
                    文化内涵: %s
                    详细介绍: %s
                    演艺/开放信息: %s
                    备注: %s
                    是否热门: %s

                    """,
                        attr.getAttrName(),
                        attr.getAttrType(),
                        attr.getLocation(),
                        attr.getHighlights(),
                        attr.getBuildingInfo(),
                        attr.getFeatures(),
                        attr.getCulture(),
                        attr.getDetail(),
                        attr.getPerformanceInfo(),
                        attr.getRemark(),
                        attr.getIsHot() == 1 ? "是" : "否"
                ));
            });
        }

        // 3. RAG 向量检索
        List<String> ragContexts = vectorStoreQueryService.query(question, 5);
        if (!ragContexts.isEmpty()) {
            combinedContext.append("【景区通用知识（来自向量库）】:\n");
            ragContexts.forEach(rag -> combinedContext.append(rag).append("\n"));
            log.info("向量库语义检索到 {} 条知识", ragContexts.size());
        }

        return combinedContext.toString();
    }

    /**
     * 增强版景点名称提取逻辑
     */
    private String extractAttractionName(String question, String currentAttraction) {
        if (question == null || question.isBlank()) {
            return null;
        }

        if (question.contains("这里") || question.contains("当前") || question.contains("这儿")) {
            return currentAttraction;
        }

        String matchedName = null;
        int maxLength = 0;

        for (String name : attractionService.getAllAttractionNames()) {
            if (question.contains(name)) {
                if (name.length() > maxLength) {
                    matchedName = name;
                    maxLength = name.length();
                }
            }
        }

        if (matchedName == null && question.length() < 10 && currentAttraction != null) {
            return currentAttraction;
        }

        return matchedName;
    }
}
