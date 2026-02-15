# 云原生博客与知识库系统

基于 Spring Boot + Vue 的微服务博客系统，容器化部署于 Kubernetes。

---

## 项目结构

```
cloud-native-blog-system/
├── docs/                 # 文档
├── gateway/              # API 网关服务
├── services/             # 业务微服务
│   ├── auth-service/     # 用户认证服务
│   ├── blog-service/     # 博客内容服务
│   ├── comment-service/  # 评论服务
│   └── (可选) file-service、search-service
├── frontend/             # 前端（Vue + Element Plus）
├── infrastructure/       # 基础设施配置与脚本
├── deploy/               # 部署相关
└── scripts/              # 脚本
```

---

## 各目录说明

| 目录 | 放什么 |
|------|--------|
| **docs/** | 任务书、技术栈说明、接口文档、设计说明等文档。 |
| **gateway/** | Spring Cloud Gateway 项目：路由、鉴权、限流等，作为统一入口。 |
| **services/auth-service/** | 用户注册、登录、JWT 签发、权限校验。 |
| **services/blog-service/** | 文章、分类、标签的增删改查，MyBatis Plus + MySQL。 |
| **services/comment-service/** | 评论的增删改查，可调用 blog-service 做文章校验。 |
| **frontend/** | Vue 3 + Vite + Element Plus 前端工程，页面与接口调用。 |
| **infrastructure/** | 基础组件配置与初始化：MySQL 建表/初始数据、Redis/Nacos 配置、ES 索引、MinIO 桶、监控配置等；本地联调可用 docker-compose 一键起 MySQL+Redis+Nacos。 |
| **deploy/** | 业务服务的 Dockerfile、Kubernetes YAML（Deployment/Service/ConfigMap 等）。 |
| **scripts/** | 构建、打包、推送镜像、部署等 Shell/PowerShell 脚本。 |

---

## 快速开始

- 详细技术选型见：[docs/技术栈.md](docs/技术栈.md)
- 后端：各服务下 `mvn spring-boot:run` 或 IDEA 运行；需先启动 MySQL、Redis、Nacos 等。
- 前端：`cd frontend && npm install && npm run dev`
- 部署：见 `deploy/` 下说明，使用 Docker + Kubernetes。

---

## 基本要求（任务书）

- 微服务不少于 3 个，全部容器化，由 Kubernetes 编排。
- 具备完整 CI/CD 流水线（如 GitHub Actions 在 `.github/workflows/`）。
