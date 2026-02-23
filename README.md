# 云原生博客与知识库系统

基于 Spring Boot 与 Vue 3 的微服务架构博客与知识库系统，支持文章发布、知识库问答、全文检索与 AI 辅助创作，后端服务容器化并由 Kubernetes 编排部署。

---

## 架构概览

- **前端**：Vue 3 + Vite + TypeScript + Element Plus，单页应用（SPA），通过 API 网关访问后端。
- **网关**：Spring Cloud Gateway 作为统一入口，负责路由、鉴权与限流。
- **微服务**：按领域拆分为用户、内容、文件、搜索、互动、AI 等独立服务，通过 Nacos 注册发现，支持 Docker 构建与 K8s 部署。
- **技术栈详情**：[docs/技术栈.md](docs/技术栈.md)

---

## 项目结构

```
cloud-native-blog-system/
├── docs/                      # 项目文档
│   ├── 技术栈.md              # 技术选型与版本说明
│   ├── API接口文档.md         # RESTful API 约定
│   ├── 数据库设计.md          # 表结构设计
│   └── 工作计划书.md          # 开发与迭代计划
├── services/                  # 后端微服务（Java / Spring Boot）
│   ├── gateway-service/       # API 网关
│   ├── user-service/          # 用户服务
│   ├── content-service/       # 内容服务
│   ├── file-service/          # 文件服务
│   ├── search-service/        # 搜索服务
│   ├── interaction-service/   # 互动服务
│   └── ai-service/            # AI 辅助服务
├── frontend/                  # 前端工程（Vue 3 + Vite + Element Plus）
├── infrastructure/             # 基础设施（MySQL/Redis/Nacos 等配置与脚本）
├── deploy/                    # 部署配置（Dockerfile、Kubernetes 清单）
└── scripts/                   # 构建、打包、部署脚本
```

---

## 微服务职责说明

| 服务 | 职责说明 | 主要能力 |
|------|----------|----------|
| **gateway-service** | API 网关，系统统一入口 | 路由转发、请求鉴权、限流与熔断、跨域与统一错误处理 |
| **user-service** | 用户与认证 | 用户注册/登录、JWT 签发与校验、会话管理、权限与角色 |
| **content-service** | 博客与知识库内容 | 文章/专栏的增删改查、分类与标签、草稿与发布、知识库内容管理 |
| **file-service** | 文件与对象存储 | 图片/附件上传、存储（如 MinIO/S3）、访问 URL 生成与鉴权 |
| **search-service** | 全文检索 | 文章/知识库的索引与全文搜索、热门与推荐、搜索建议 |
| **interaction-service** | 互动与社交 | 评论、点赞、收藏、关注、动态流与消息通知 |
| **ai-service** | AI 辅助能力 | 标题生成、摘要提取、智能推荐、知识库问答等（可对接大模型 API） |

- **gateway-service** 将外部请求按路径前缀转发至上述各业务服务，业务服务之间可通过 OpenFeign 或 HTTP 客户端按需调用。
- 各服务独立数据库（或独立 Schema），避免表级耦合；缓存、消息队列等按需在对应服务或基础设施中配置。

---

## 目录与文档说明

| 目录/文档 | 说明 |
|-----------|------|
| **docs/** | 技术栈说明、API 接口文档、数据库设计、工作计划等。 |
| **services/gateway-service** | Spring Cloud Gateway：路由、鉴权、限流，依赖 Nacos 做服务发现。 |
| **services/user-service** | 用户与认证：注册、登录、JWT、Spring Security。 |
| **services/content-service** | 文章、分类、标签、知识库内容，持久层可用 MyBatis Plus + MySQL。 |
| **services/file-service** | 文件上传与对象存储对接（如 MinIO）。 |
| **services/search-service** | 全文检索，可对接 Elasticsearch 等。 |
| **services/interaction-service** | 评论、点赞、收藏、关注等互动能力。 |
| **services/ai-service** | AI 能力封装，如标题/摘要生成、知识库问答。 |
| **frontend/** | Vue 3 + Vite + Element Plus 前端，负责页面与接口调用。 |
| **infrastructure/** | MySQL/Redis/Nacos 等配置、建表脚本、docker-compose 本地环境。 |
| **deploy/** | 各服务 Dockerfile、Kubernetes Deployment/Service/ConfigMap 等。 |
| **scripts/** | 构建、打包镜像、推送仓库、部署到 K8s 的 Shell/PowerShell 脚本。 |

---

## 快速开始

### 环境要求

- **JDK** 17+
- **Node.js** 18+（前端）
- **Maven** 3.8+
- **MySQL** 8.x、**Redis**、**Nacos**（或按 infrastructure 说明启动）

### 后端服务

在对应服务目录下启动（需先启动 MySQL、Redis、Nacos）：

```bash
cd services/user-service   # 或其他服务
mvn spring-boot:run
```

也可使用 IDE 运行各服务的 `Application` 主类。网关需在业务服务之后或同时启动，以便正确路由。

### 前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问开发环境地址（如 `http://localhost:5173`），前端通过配置的 API 基地址访问网关。

### 部署

- 镜像构建与 Kubernetes 部署方式见 **deploy/** 目录。
- 本地或 CI 中可使用 **scripts/** 下脚本完成构建、推送镜像及部署。

---

## 文档索引

- [技术栈说明](docs/技术栈.md)  
- [API 接口文档](docs/API接口文档.md)  
- [数据库设计](docs/数据库设计.md)  
- [工作计划书](docs/工作计划书.md)  

---

## 许可证

本项目仅供学习与毕业设计使用。
