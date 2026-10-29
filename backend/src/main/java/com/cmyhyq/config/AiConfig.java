package com.cmyhyq.config;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

    @Value("${spring.ai.vectorstore.pgvector.host}")
    private String pgHost;

    @Value("${spring.ai.vectorstore.pgvector.port}")
    private int pgPort;

    @Value("${spring.ai.vectorstore.pgvector.database}")
    private String pgDatabase;

    @Value("${spring.ai.vectorstore.pgvector.username}")
    private String pgUsername;

    @Value("${spring.ai.vectorstore.pgvector.password}")
    private String pgPassword;

    @Value("${app.dashscope.api-key}")
    private String apiKey;

    @Value("${app.dashscope.model}")
    private String model;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;


    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", pgHost, pgPort, pgDatabase));
        dataSource.setUsername(pgUsername);
        dataSource.setPassword(pgPassword);
        return new JdbcTemplate(dataSource);
    }


    /**
     * 向量库
     * @param embeddingModel
     * @return
     */
    @Bean
    public PgVectorStore pgVectorStore(EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(pgVectorJdbcTemplate(), embeddingModel)
                .initializeSchema(true)
                .build();
    }


    // ... existing code ...

    /**
     * 【核心】配置 HTTP 请求超时
     * 连接超时：120秒
     * 读取超时：300秒（5分钟）- 解决游记生成超时问题
     */
    @Bean
    @Primary
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(120000); // 120秒
        factory.setReadTimeout(300000);    // 300秒
        return factory;
    }

    /**
     * 配置 OpenAI API
     * Spring AI 会自动使用带有 @Primary 的 ClientHttpRequestFactory
     */
    @Bean
    public org.springframework.ai.openai.api.OpenAiApi openAiApi() {
        return new OpenAiApi.Builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }


    /**

    /**
     * 对话模型
     * @param chatModel
     * @return
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 初始化阿里云多模态模型 SDK
     */
    @Bean
    public MultiModalConversation multiModalConversation() {

        System.setProperty("dashscope.api-key", apiKey);
        System.setProperty("dashscope.model", model);
        return new MultiModalConversation();

    }



}
