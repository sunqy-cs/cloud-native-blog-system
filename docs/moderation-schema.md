# 内容审核数据模型（统一表）

## `moderation_task`

| 字段 | 说明 |
|------|------|
| `resource_type` | `ARTICLE` 普通博客（content.type=BLOG）<br>`KNOWLEDGE_DOC` 公开知识库文档（content.type=KNOWLEDGE）<br>`COMMENT` 评论<br>`USER_PROFILE` 用户资料变更 |
| `resource_id` | 对应 `content.id` / `comment.id` / `user.id` |
| `owner_user_id` | 提交者；**管理员账号不产生任务**（应用层不写表） |
| `status` | `PENDING` → AI → `NEEDS_HUMAN` 或 `APPROVED`/`REJECTED` |
| `ai_*` / `human_*` | AI 与人工结论及时间 |
| `payload_snapshot` | JSON 送审快照（标题、摘要、资料字段等） |

## 业务表冗余字段（便于列表/过滤）

- `content.moderation_status`
- `comment.moderation_status`
- `user.profile_moderation_status`

与 `moderation_task` 由业务层保持最终一致；管理员发布内容不设 `moderation_task` 且可保持 `moderation_status` 为空或 APPROVED。

## 前端 API（预留）

- `GET /api/admin/moderation/tasks` 分页列表
- `GET /api/admin/moderation/stats` 首页指标

网关已预留 `Path=/api/admin/moderation/**` → `content-service`（可按实际服务名调整）。
