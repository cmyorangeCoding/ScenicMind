# admin-web · 景区运营管理后台

Vue 3 + Vite + Element Plus + Pinia + ECharts 构建的管理端，覆盖数据大屏、游客感受度报告、用户管理、投诉工单、景点库、知识库、高频问题、数字人管理与账号管理。

## 本地运行

```bash
npm install
npm run dev      # http://localhost:5173，/api 自动代理到 http://localhost:8080
npm run build    # 产物输出到 dist/
```

## 说明

- 接口基地址为 `/api`（见 `src/api/http.js`），由 Vite 开发代理或 `deploy/nginx/admin.conf` 转发到后端并去掉 `/api` 前缀。
- 演示账号：`admin / admin123`（对应 `deploy/sql/mysql/02-demo-data.sql` 中的种子数据）。
- 生产部署请修改 `deploy/.env` 中的密钥与数据库密码，不要使用默认值。
