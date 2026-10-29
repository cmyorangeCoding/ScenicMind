# backend · Spring Boot 3 + Spring AI 服务端

## 模块结构

```
src/main/java/com/cmyhyq/
├── ai/           MasterAgent、QaAgent / RouteAgent / ProfileAgent / AnalyzeAgent / GuideAgent
│   ├── rag/      向量同步与知识检索
│   ├── tools/    Spring AI @Tool（天气 / 知识 / 路线）
│   └── prompt/   系统提示词
├── controller/   admin 与 user 两套接口
├── service/      业务服务
├── mapper/       MyBatis-Plus Mapper
├── pojo/         entity / dto / vo
├── config/       AI、鉴权、WebSocket、Redis、跨域
├── websocket/    ChatWebSocket 流式对话
└── util/         JWT、OSS、语音、雪花 ID
```

## 本地运行

```bash
# 1. 准备 MySQL 8 / Redis 7 / PostgreSQL 16（含 pgvector 扩展）
# 2. 导入 deploy/sql 下的初始化脚本
# 3. 配置环境变量（或直接改 application.yml 的 guide.* 默认值）
# 4. 启动
./mvnw spring-boot:run
```

默认端口 `8080`，WebSocket 路径 `ws://host:8080/ws/chat`。

## 配置

所有敏感项通过环境变量注入，变量清单见 `deploy/.env.example`。
`DASHSCOPE_API_KEY` 为必填项，其余第三方能力可按需开启。

> 向量表 `ai_vector_store` 由 Spring AI 首次启动时自动创建；示例知识需在管理端执行一次“编辑保存”完成双写。
