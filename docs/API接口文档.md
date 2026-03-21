# API 接口文档（RESTful）

## 基础说明

- **Base URL**: `/api`
- **Content-Type**: `application/json`
- **认证方式**: 登录后在请求头携带 `Authorization: Bearer {token}`

---

## 认证相关

### 1. 发送短信验证码

**`POST /api/auth/sms/send`**

向指定手机号发送短信验证码（登录或注册场景）。前端在「验证码登录」或「注册」时，用户点击「获取验证码」后调用。  
**无需登录。**

**Request Body**:

```json
{
  "phone": "13800138000",
  "scene": "LOGIN"
}
```

| 字段   | 类型   | 必填 | 说明 |
|--------|--------|------|------|
| phone  | string | 是   | 中国大陆 11 位手机号（建议正则：`^1\d{10}$`） |
| scene  | string | 是   | `LOGIN`：验证码登录；`REGISTER`：注册；`RESET_PASSWORD`：找回/重置密码 |

**Response** `204 No Content`（成功无 body）。

**后端实现说明（user-service，阿里云号码认证）**：

- 调用阿里云 **SendSmsVerifyCode**（OpenAPI：`dypnsapi.aliyuncs.com`，需 RAM 授权 `dypns:SendSmsVerifyCode`）。
- 须在 `application.yml` 配置 `aliyun.dypns.*`：`enabled=true`、AccessKey、**控制台赠送的** `sign-name` / `template-code`，以及 `template-param-json`（模板变量须与控制台一致；使用 `{"code":"##code##","min":"5"}` 时由阿里云生成验证码，便于服务端调用 **CheckSmsVerifyCode** 核验）。
- 注册与验证码登录时，服务端在写库/发 Token 前会调用 **CheckSmsVerifyCode** 校验用户提交的 `smsCode`（无需自建 Redis 存码，与官方文档一致）。
- 频控、有效时间等与阿里云接口 `Interval`、`ValidTime` 等参数及运营商策略一致；失败时返回 `400` 及可读 `message`。

---

### 2. 通过手机验证码重置密码

**`POST /api/auth/password/reset`**

已绑定手机号的用户忘记密码时，通过短信验证码设置新密码。**无需登录。**

须先调用 **`POST /api/auth/sms/send`**，且 **`scene` 必须为 `RESET_PASSWORD`**。

**Request Body**：

```json
{
  "phone": "13800138000",
  "smsCode": "123456",
  "newPassword": "新密码至少6位"
}
```

| 字段        | 类型   | 必填 | 说明 |
|-------------|--------|------|------|
| phone       | string | 是   | 11 位手机号，须为已注册账号 |
| smsCode     | string | 是   | 与 `RESET_PASSWORD` 场景下发的验证码一致 |
| newPassword | string | 是   | 新密码，至少 6 位 |

**Response** `204 No Content`。若手机号未注册返回 `400`。

---

### 3. 创建会话（登录）

**`POST /api/sessions`**

创建登录会话，获取访问令牌。支持 **两种方式二选一**（不可混用）：

**方式 A：用户名 + 密码**

```json
{
  "username": "string",
  "password": "string"
}
```

| 字段     | 类型   | 必填 | 说明   |
|----------|--------|------|--------|
| username | string | 是*  | 用户名 |
| password | string | 是*  | 密码   |

**方式 B：手机号 + 短信验证码**

```json
{
  "phone": "13800138000",
  "smsCode": "123456"
}
```

| 字段    | 类型   | 必填 | 说明 |
|---------|--------|------|------|
| phone   | string | 是*  | 已与账号绑定的 11 位手机号 |
| smsCode | string | 是*  | 通过 `POST /api/auth/sms/send`（`scene=LOGIN`）获取的验证码 |

\* 方式 A 与方式 B 互斥：请求体要么包含 `username`+`password`，要么包含 `phone`+`smsCode`。

**Response** `201 Created`:

```json
{
  "token": "string",
  "user": {
    "id": 1,
    "username": "string",
    "nickname": "string"
  }
}
```

或兼容格式：

```json
{
  "data": {
    "token": "string"
  }
}
```

---

### 4. 创建用户（注册）

**`POST /api/users`**

创建新用户账号。**必须**校验手机号与短信验证码（`scene=REGISTER`）有效且匹配，否则返回 `400`。**无需登录。**

**Request Body**:

```json
{
  "username": "string",
  "password": "string",
  "phone": "13800138000",
  "smsCode": "123456"
}
```

| 字段     | 类型   | 必填 | 说明           |
|----------|--------|------|----------------|
| username | string | 是   | 用户名，唯一   |
| password | string | 是   | 密码，至少 6 位 |
| phone    | string | 是   | 11 位手机号，注册成功后写入用户表，建议唯一 |
| smsCode  | string | 是   | 与 `phone` 对应、且 `scene=REGISTER` 下发验证码 |

**Response** `201 Created`:

```json
{
  "id": 1,
  "username": "string",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

或 `204 No Content` 表示创建成功。

---

### 4. 获取当前用户

**`GET /api/users/me`**

需要认证。获取当前登录用户信息。

**Response** `200 OK`:

```json
{
  "id": 1,
  "username": "string",
  "nickname": "string",
  "avatar": "string",
  "cover": "string",
  "gender": "string",
  "intro": "string",
  "residence": "string",
  "industry": "string",
  "bio": "string",
  "phone": "138****8000",
  "role": "string",
  "createdAt": "2025-01-01T00:00:00"
}
```

| 字段       | 类型   | 说明                 |
|------------|--------|----------------------|
| id         | number | 用户 ID              |
| username   | string | 用户名               |
| nickname   | string | 昵称，可选           |
| avatar     | string | 头像 URL，可选       |
| cover      | string | 背景图/封面 URL，可选 |
| gender     | string | 性别，可选           |
| intro      | string | 一句话介绍，可选     |
| residence  | string | 居住地，可选         |
| industry   | string | 所在行业，可选       |
| bio        | string | 个人简介，可选       |
| phone      | string | 绑定手机号，可选脱敏 |
| role       | string | 角色                 |
| createdAt  | string | 创建时间             |

---

### 5. 更新当前用户资料（编辑个人资料）

**`PATCH /api/users/me`**

需要认证。更新当前用户资料，请求体中的字段均为可选，仅更新提供的字段；传空字符串可清空该字段。

**Request Body**:

```json
{
  "nickname": "string",
  "avatar": "https://example.com/avatar.jpg",
  "cover": "https://example.com/cover.jpg",
  "gender": "男",
  "intro": "一句话介绍",
  "residence": "现居深圳市",
  "industry": "计算机软件",
  "bio": "个人简介内容"
}
```

| 字段      | 类型   | 必填 | 说明                     |
|-----------|--------|------|--------------------------|
| nickname  | string | 否   | 昵称                     |
| avatar    | string | 否   | 头像 URL                 |
| cover     | string | 否   | 背景图/封面 URL（上传后填入） |
| gender    | string | 否   | 性别：男/女/其他         |
| intro     | string | 否   | 一句话介绍               |
| residence | string | 否   | 居住地                   |
| industry  | string | 否   | 所在行业                 |
| bio       | string | 否   | 个人简介                 |

> **手机号**：不建议在本接口直接修改；换绑宜使用单独的验证码换绑接口。

**Response** `200 OK`: 返回更新后的用户对象，格式同 `GET /api/users/me`。

---

## 内容/博客相关

### 6. 分页获取当前用户的博客列表（我的博客）

**`GET /api/contents/me`**

需要认证。获取当前登录用户创建的博客/内容列表，用于个人页「我的博客」展示。仅返回类型为博客（BLOG）的内容；可包含草稿与已发布。

**Query 参数**:

| 参数       | 类型   | 必填 | 说明           |
|------------|--------|------|----------------|
| page       | number | 否   | 页码，从 1 开始，默认 1 |
| pageSize   | number | 否   | 每页条数，默认 10      |
| visibility | string | 否   | 可见范围筛选：ALL-全部可见 / SELF-仅我可见 / FANS-粉丝可见；不传则不过滤 |
| status     | string | 否   | 状态筛选：ALL-全部 / PUBLISHED-已发布 / REJECTED-审核不通过 / DRAFT-草稿；不传则不过滤 |
| sortBy     | string | 否   | 排序字段：time-按发布时间 / likes-按点赞量 / views-按浏览量，默认 time |
| order      | string | 否   | 排序方向：asc-升序 / desc-降序，默认 desc |
| q          | string | 否   | 搜索关键词；传则只返回标题、摘要或标签中**模糊匹配**该词的内容（用于个人主页「搜索 TA 的创作」）。此时列表中每项可能带 `tagNames` 供前端高亮。 |
| columnId   | number | 否   | 按专栏 ID 筛选；不传则返回全部。用于博客页「HOME / 专栏」导航筛选。 |

**Response** `200 OK`:

```json
{
  "list": [
    {
      "id": 1,
      "title": "string",
      "summary": "string",
      "cover": "string",
      "status": "PUBLISHED",
      "articleType": "ORIGINAL",
      "viewCount": 0,
      "likeCount": 0,
      "collectionCount": 0,
      "commentCount": 0,
      "createdAt": "2026-02-15"
    }
  ],
  "total": 5
}
```

| 字段            | 类型   | 说明         |
|-----------------|--------|--------------|
| list            | array  | 当前页数据   |
| list[].id       | number | 内容 ID      |
| list[].title    | string | 标题         |
| list[].summary  | string | 摘要，可选   |
| list[].cover    | string | 封面图 URL，可选 |
| list[].status   | string | 状态：DRAFT-草稿 / PUBLISHED-已发布 / REJECTED-审核不通过 |
| list[].articleType | string | 文章类型：ORIGINAL-原创 / REPRINT-转载 / TRANSLATED-翻译 |
| list[].viewCount      | number | 浏览数 |
| list[].likeCount      | number | 点赞数 |
| list[].collectionCount| number | 收藏数 |
| list[].commentCount   | number | 评论数 |
| list[].createdAt| string | 创建时间，格式 YYYY-MM-DD 或 ISO 8601 |
| list[].tagNames | string[] | 标签名称列表；仅在请求带 `q` 时可能返回，用于前端高亮匹配标签 |
| total           | number | 总条数（符合条件的内容总数） |

---

### 6.1 保存草稿

**`POST /api/contents/draft`**

需要认证。将当前编辑内容保存为草稿。**博客草稿**：正文（body）不能为空，否则返回 400。**知识库内容**（传 id 且该内容类型为 KNOWLEDGE 时）：允许正文为空。若标题（title）为空，服务端将标题存为「[无标题]」。

**Request Body**:

```json
{
  "title": "string",
  "body": "string",
  "summary": "string",
  "cover": "string",
  "columnId": 0,
  "articleType": "ORIGINAL",
  "creationStatement": "none",
  "visibility": "ALL",
  "tagNames": ["大模型与对齐（LLM/RLHF/DPO）", "自定义标签"]
}
```

| 字段              | 类型     | 必填 | 说明 |
|-------------------|----------|------|------|
| title             | string   | 否   | 标题；空或不传时存为「[无标题]」 |
| body              | string   | 博客必填 | 正文（Markdown）。博客草稿为空时返回 400；知识库内容允许为空 |
| summary           | string   | 否   | 摘要 |
| cover             | string   | 否   | 封面图 URL |
| columnId          | number   | 否   | 所属专栏 ID |
| articleType       | string   | 否   | 文章类型：ORIGINAL-原创 / REPRINT-转载 / TRANSLATED-翻译，默认 ORIGINAL |
| creationStatement | string   | 否   | 创作声明（可选，不强制）：none-无声明 / ai-assisted-部分内容由AI辅助生成 / network-内容来源网络 / personal-个人观点仅供参考，默认 none |
| visibility        | string   | 否   | 可见范围：ALL-全部可见 / SELF-仅我可见 / FANS-粉丝可见，默认 ALL |
| tagNames          | string[] | 否   | 文章标签名称列表，最多 5 个。保存时后端按名称查询标签，不存在则创建（is_main=0）再建立 content_tag 关联 |

**Response** `201 Created`:

```json
{
  "id": 1,
  "title": "[无标题]",
  "status": "DRAFT",
  "createdAt": "2026-02-21T10:00:00"
}
```

| 字段      | 类型   | 说明 |
|-----------|--------|------|
| id        | number | 内容 ID（草稿） |
| title     | string | 保存后的标题 |
| status    | string | 固定为 DRAFT |
| createdAt | string | 创建时间 |

**错误**：若 `body` 为空或仅空白，返回 `400 Bad Request`，报错信息如「正文不能为空」。

**说明**：请求体可带 `id`（内容 ID）。若传 `id` 且该内容属于当前用户，则更新该条内容；不传则新建草稿。

---

### 6.2 获取编辑用内容详情

**`GET /api/contents/{id}`**

需要认证。获取指定内容的完整信息（含正文、标签等），仅允许内容作者本人调用，用于创作页「编辑」回填。

**Path 参数**：`id` 内容 ID。

**Response** `200 OK`：

```json
{
  "id": 1,
  "title": "string",
  "body": "string",
  "summary": "string",
  "cover": "string",
  "columnId": 0,
  "articleType": "ORIGINAL",
  "creationStatement": "none",
  "visibility": "ALL",
  "tagNames": ["主标签名", "其他标签1", "其他标签2"]
}
```

| 字段              | 类型     | 说明 |
|-------------------|----------|------|
| id                | number   | 内容 ID |
| title             | string   | 标题 |
| body              | string   | 正文（Markdown） |
| summary           | string   | 摘要 |
| cover             | string   | 封面 URL |
| columnId          | number   | 专栏 ID |
| articleType       | string   | ORIGINAL / REPRINT / TRANSLATED |
| creationStatement | string   | none / ai-assisted / network / personal |
| visibility        | string   | ALL / SELF / FANS |
| tagNames          | string[] | 标签名称列表，第一个为主标签，其余为其他标签 |

**错误**：内容不存在或非本人，返回 `404 Not Found`。

---

### 6.2.1 仅更新内容标题

**`PATCH /api/contents/{id}`**

需要认证。仅更新指定内容的标题；仅允许内容作者本人操作。用于知识库内文件重命名（行内编辑后保存）。

**Path 参数**：`id` 内容 ID。

**Request Body**：

```json
{
  "title": "新标题"
}
```

| 字段  | 类型   | 必填 | 说明     |
|-------|--------|------|----------|
| title | string | 否   | 新标题；空则存为「[无标题]」 |

**Response** `200 OK`：无响应体。

**错误**：内容不存在或非本人返回 `404`。

---

### 6.2.2 双链笔记：获取反链

**`GET /api/contents/{id}/backlinks`**

无需认证。返回「引用了该内容」的笔记列表（即正文中包含指向该内容的 `[[id:标题]]` 的笔记），用于双链笔记的「被引用」展示。

**Path 参数**：`id` 内容 ID。

**Response** `200 OK`：与「我的博客」列表项格式一致（id、title、userId、viewCount 等）。

**`GET /api/contents/{id}/outlinks`**

无需认证。返回「该内容引出的笔记」列表（正文中的 `[[id:标题]]` 指向的笔记），用于双链右侧栏「出链」列表。

**Response** `200 OK`：与 backlinks 格式一致。

---

#### 双链：添加/删除引用（需认证）

**`POST /api/contents/{id}/references`** — 添加出链（当前内容 → 目标内容）

需要认证。Request Body：`{ "targetId": number }`。仅允许当前内容作者操作。

**`DELETE /api/contents/{id}/references/{targetId}`** — 删除出链

需要认证。仅允许当前内容作者操作。

**`POST /api/contents/{id}/backlinks`** — 添加入链（来源内容 → 当前内容）

需要认证。Request Body：`{ "sourceId": number }`。仅允许来源内容作者操作。

**`DELETE /api/contents/{id}/backlinks/{sourceId}`** — 删除入链

需要认证。仅允许来源内容作者操作。

---

### 6.3 发布博客

**`POST /api/contents/{id}/publish`**

需要认证。将指定内容从草稿状态发布为正式文章（状态改为 PUBLISHED）。仅允许内容作者本人操作，且该内容当前必须为草稿（DRAFT）。

**Path 参数**：`id` 内容 ID。

**Request Body**：无（或 `{}`）。

**Response** `200 OK`：

```json
{
  "id": 1,
  "title": "文章标题",
  "status": "PUBLISHED",
  "publishedAt": "2026-02-21T14:00:00"
}
```

| 字段         | 类型   | 说明 |
|--------------|--------|------|
| id           | number | 内容 ID |
| title        | string | 标题 |
| status       | string | 固定为 PUBLISHED |
| publishedAt  | string | 发布时间（与 updatedAt 一致或由服务端生成） |

**错误**：
- 内容不存在或非本人：`404 Not Found`
- 内容已是已发布状态：`400 Bad Request`（如「该内容已发布」）

**说明**：前端流程建议：先调用「保存草稿」确保当前编辑内容入库（得到或刷新 `id`），再调用本接口发布该 `id`。

---

## 标签相关（content-service）

### 获取主标签列表

**`GET /api/tags/main`**

需要认证。获取全部主标签（12 个），用于创作页「文章标签」必须选择其一。再配合「其他标签」：从非主标签列表选择或 AI 生成，合计最多 5 个标签。

**Response** `200 OK`:

```json
[
  { "id": 1, "name": "机器学习理论与优化" },
  { "id": 2, "name": "深度学习架构与表示学习" },
  { "id": 3, "name": "大模型与对齐（LLM/RLHF/DPO）" },
  { "id": 12, "name": "其他" }
]
```

| 字段  | 类型   | 说明     |
|-------|--------|----------|
| id    | number | 标签 ID  |
| name  | string | 标签名称 |

---

### 获取非主标签列表

**`GET /api/tags/others`**

需要认证。获取全部非主标签（is_main=0），即用户或系统已创建的非一级标签，用于创作页「其他标签」从已有标签中多选。与主标签合计最多 5 个。

**Response** `200 OK`:

```json
[
  { "id": 13, "name": "PyTorch" },
  { "id": 14, "name": "论文笔记" }
]
```

字段说明同「获取主标签列表」。

---

## 收藏夹相关

### 7. 获取当前用户的收藏夹列表

**`GET /api/collection-folders/me`**

需要认证。获取当前登录用户创建的所有收藏夹，用于个人页「我的收藏」展示。每个收藏夹返回其名称、简介、是否默认、该收藏夹内收藏条数及创建时间。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`，由网关鉴权后注入，具体以网关实现为准）。

**Response** `200 OK`:

```json
[
  {
    "id": 1,
    "name": "默认收藏夹",
    "description": "",
    "isDefault": true,
    "count": 2,
    "createdAt": "2026-02-20"
  },
  {
    "id": 2,
    "name": "读书清单",
    "description": "想读和已读的书",
    "isDefault": false,
    "count": 0,
    "createdAt": "2026-02-21"
  }
]
```

| 字段        | 类型    | 说明           |
|-------------|---------|----------------|
| id          | number  | 收藏夹 ID      |
| name        | string  | 收藏夹名称     |
| description | string  | 收藏夹简介，可选 |
| isDefault   | boolean | 是否默认收藏夹 |
| count       | number  | 该收藏夹内收藏的内容条数 |
| createdAt   | string  | 创建时间，格式 YYYY-MM-DD |

---

### 8. 创建收藏夹

**`POST /api/collection-folders`**

需要认证。创建新收藏夹，仅支持设置名称与简介，不涉及可见性。

**Request Body**:

```json
{
  "name": "string",
  "description": "string"
}
```

| 字段        | 类型   | 必填 | 说明           |
|-------------|--------|------|----------------|
| name        | string | 是   | 收藏夹名称     |
| description | string | 否   | 收藏夹简介     |

**Response** `201 Created`:

```json
{
  "id": 3,
  "name": "读书清单",
  "description": "想读和已读的书",
  "isDefault": false,
  "count": 0,
  "createdAt": "2026-02-21"
}
```

返回体字段说明同「获取当前用户的收藏夹列表」单条。

---

### 9. 更新收藏夹

**`PATCH /api/collection-folders/{id}`**

需要认证。修改收藏夹的名称和/或简介。**默认收藏夹不允许修改名字和简介**，请求会返回 `400 Bad Request`。

**路径参数**:

| 参数 | 类型   | 说明     |
|------|--------|----------|
| id   | number | 收藏夹 ID |

**Request Body**（字段均可选，只更新传入的字段）:

```json
{
  "name": "string",
  "description": "string"
}
```

| 字段        | 类型   | 必填 | 说明       |
|-------------|--------|------|------------|
| name        | string | 否   | 收藏夹名称 |
| description | string | 否   | 收藏夹简介 |

**Response** `200 OK`: 返回更新后的收藏夹对象，格式同「获取当前用户的收藏夹列表」单条。

**错误**：若为默认收藏夹，返回 `400`，报错信息为「默认收藏夹不能修改名字和简介」。若收藏夹不存在或非当前用户，返回 `404`。

---

### 10. 删除收藏夹

**`DELETE /api/collection-folders/{id}`**

需要认证。删除指定收藏夹；仅允许删除本人创建的收藏夹。**默认收藏夹不可删除**。该收藏夹下的收藏关系会随表外键一并删除。

**路径参数**:

| 参数 | 类型 | 说明     |
|------|------|----------|
| id   | number | 收藏夹 ID |

**Response** `204 No Content`

若收藏夹不存在或非当前用户创建，返回 `404 Not Found`。若为默认收藏夹，返回 `400`，报错信息为「不能删除默认收藏夹」。

---

### 11. 注销会话（登出）

**`DELETE /api/sessions/current`**

需要认证。使当前 Token 失效。

**Response** `204 No Content`

> 前端也可仅清除本地 Token 完成登出，无需调用此接口。

---

## 动态 / 赞同相关（interaction-service）

个人页「我的动态」需要展示「赞同了文章」与「发表了博客」的混合时间线。赞同数据由 interaction-service 提供，内容摘要由 content-service 提供。

### 12. 分页获取当前用户赞同的内容列表（我赞同的文章）

**`GET /api/content-likes/me`**

需要认证。获取当前登录用户点赞过的内容 ID 及点赞时间，用于个人页动态「赞同了文章」展示。前端可再根据 `contentId` 调用「按 ID 批量获取内容」拿到标题、摘要等。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Query 参数**：

| 参数     | 类型   | 必填 | 说明                    |
|----------|--------|------|-------------------------|
| page     | number | 否   | 页码，从 1 开始，默认 1  |
| pageSize | number | 否   | 每页条数，默认 10        |

**Response** `200 OK`：

```json
{
  "list": [
    {
      "contentId": 1,
      "likedAt": "2026-02-23T11:33:00"
    }
  ],
  "total": 5
}
```

| 字段              | 类型   | 说明           |
|-------------------|--------|----------------|
| list              | array  | 当前页数据     |
| list[].contentId  | number | 内容 ID        |
| list[].likedAt    | string | 点赞时间       |
| total             | number | 赞同总数       |

---

## 内容服务补充（content-service）

### 12.1 博客可见性（visibility）校验

博客字段 `visibility`：**ALL** 全部可见、**SELF** 仅作者可见、**FANS** 仅作者与粉丝可见。

- **`GET /api/contents/list`**（推荐列表 / 他人博客列表）：当传入 `userId`（查看他人博客）时，会根据请求头 `X-User-Id`（当前登录用户）过滤：未登录仅返回 `visibility=ALL`；登录且为本人返回全部；登录且为他人时仅返回 `ALL` 以及（若当前用户已关注该作者）`FANS`，不返回 `SELF`。
- **`GET /api/contents/view/{id}`**（文章阅读）：进入正文前按 `visibility` 校验：`SELF` 仅作者可读；`FANS` 仅作者或已关注作者的粉丝可读；无权限时返回 `404`（与「内容不存在」一致，不泄露是否存在该文）。

**关注校验**（供 content-service 内部调用）：**`GET /api/follow/check?followeeId={被关注用户ID}`**，请求头 `X-User-Id` 为当前用户（关注方）。返回 `{ "following": true/false }`，用于判断 FANS 是否可见。

---

### 13. 按 ID 批量获取内容摘要（用于动态等）

**`GET /api/contents/by-ids`**

需要认证。根据内容 ID 列表批量返回摘要信息（标题、摘要、创建时间等），用于动态里展示「赞同了文章」的标题与跳转。

**Query 参数**：

| 参数 | 类型   | 必填 | 说明                         |
|------|--------|------|------------------------------|
| ids  | string | 是   | 内容 ID，多个用英文逗号分隔，如 `1,2,3` |

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "title": "string",
    "summary": "string",
    "cover": "string",
    "viewCount": 0,
    "likeCount": 0,
    "collectionCount": 0,
    "createdAt": "2026-02-15"
  }
]
```

返回顺序与请求 `ids` 顺序一致；已删除或不存在的 ID 不返回对应项。

---

### 14. 获取有评论的文章列表（评论管理左侧）

**`GET /api/comments/commented-articles`**（interaction-service）

需要认证。获取当前用户作为作者、且至少有一条评论的文章列表，用于创作者中心「评论管理」左侧列表。按更新时间倒序。

**Response** `200 OK`：

```json
[
  {
    "contentId": 1,
    "title": "文章标题",
    "commentCount": 3,
    "lastCommentAt": "2026-02-26 13:42"
  }
]
```

| 字段            | 类型   | 说明           |
|-----------------|--------|----------------|
| contentId       | number | 内容 ID        |
| title           | string | 文章标题       |
| commentCount    | number | 该文章评论数   |
| lastCommentAt   | string | 最后评论时间   |

---

### 15. 获取某篇文章的评论列表（评论管理右侧 / 文章页评论区）

**`GET /api/comments/list?contentId={contentId}`**（interaction-service）

返回该文章下所有评论，热评在前、再按时间倒序。已发布的博客对所有人可见；未发布仅作者可见。请求头可带 `X-User-Id`（可选），用于返回当前用户是否已点赞每条评论（`likedByMe`）。

**Query 参数**：`contentId` 内容 ID。

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "userId": 2,
    "userNickname": "用户2",
    "userAvatar": "https://example.com/avatar.jpg",
    "contentId": 1,
    "body": "评论内容",
    "parentId": null,
    "isHot": true,
    "createdAt": "2026-02-26 13:42",
    "isAuthor": false,
    "likeCount": 3,
    "likedByMe": false
  }
]
```

| 字段          | 类型    | 说明                         |
|---------------|---------|------------------------------|
| id            | number  | 评论 ID                      |
| userId        | number  | 评论者用户 ID                |
| userNickname  | string  | 评论者昵称                   |
| userAvatar    | string  | 评论者头像 URL，可选          |
| contentId     | number  | 所属内容 ID                  |
| body          | string  | 评论正文                     |
| parentId      | number  | 父评论 ID，回复时存在        |
| isHot         | boolean | 是否热评（作者推荐）          |
| createdAt     | string  | 评论时间                     |
| isAuthor      | boolean | 是否文章作者本人评论         |
| likeCount     | number  | 点赞数                       |
| likedByMe     | boolean | 当前用户是否已点赞该条评论（未登录或未传 X-User-Id 时为 false） |

---

### 15.1 发表评论

**`POST /api/comments`**（interaction-service）

需要认证。在指定文章下发表评论或回复某条评论。仅允许对**已发布**的博客（content）评论；可选 `parentId` 表示回复某条评论。

**Request Body**：

```json
{
  "contentId": 1,
  "body": "评论内容",
  "parentId": null
}
```

| 字段      | 类型   | 必填 | 说明 |
|-----------|--------|------|------|
| contentId | number | 是   | 文章（内容）ID |
| body      | string | 是   | 评论正文，建议 1～500 字 |
| parentId  | number | 否   | 父评论 ID；不传或 null 表示顶级评论，传则表示**回复**该条评论（回复评论功能） |

**Response** `201 Created`：

```json
{
  "id": 10,
  "userId": 2,
  "userNickname": "用户2",
  "contentId": 1,
  "body": "评论内容",
  "parentId": null,
  "isHot": false,
  "createdAt": "2026-02-26 14:00",
  "isAuthor": false
}
```

返回体字段说明同「14. 获取某篇文章的评论列表」单条。

**错误**：

- `400 Bad Request`：`body` 为空或仅空白；或 `parentId` 对应的评论不存在、不属于本 `contentId`；或该内容未发布（非 PUBLISHED）、非博客（非 BLOG）。
- `401 Unauthorized`：未登录。
- `404 Not Found`：`contentId` 对应内容不存在。

---

### 15.2 点赞评论

**`POST /api/comments/{id}/like`**（interaction-service）

需要认证。对指定评论点赞（同一用户重复调用视为已点赞，幂等）。

**路径参数**：`id` 评论 ID。

**Request Body**：无（或 `{}`）。

**Response** `204 No Content`

**错误**：评论不存在返回 `404 Not Found`。

---

### 15.3 取消点赞评论

**`DELETE /api/comments/{id}/like`**（interaction-service）

需要认证。取消对指定评论的点赞。

**路径参数**：`id` 评论 ID。

**Response** `204 No Content`

**错误**：评论不存在返回 `404 Not Found`。

---

### 16. 设置/取消热评

**`PATCH /api/comments/{id}/hot`**（interaction-service）

需要认证。仅文章作者可操作。将某条评论设为热评或取消热评。

**路径参数**：`id` 评论 ID。

**Request Body**：

```json
{ "hot": true }
```

| 字段 | 类型    | 说明                |
|------|---------|---------------------|
| hot  | boolean | true-设为热评，false-取消热评 |

**Response** `204 No Content`

---

## 专栏相关（content-service）

个人页「我的专栏」展示当前用户创建的专栏列表，支持新建专栏（名称、描述、封面）。他人博客页顶栏「全部 / 专栏」需按用户 ID 拉取该用户的专栏列表（公开接口）。

### 16.1 按用户 ID 获取专栏列表（公开）

**`GET /api/columns/list`**

无需认证。根据用户 ID 获取该用户创建的所有专栏，用于他人博客页顶栏「全部 / 专栏名」导航展示；返回格式与「获取当前用户的专栏列表」单条一致。

**Query 参数**：

| 参数   | 类型   | 必填 | 说明     |
|--------|--------|------|----------|
| userId | number | 是   | 用户 ID  |

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "name": "技术笔记",
    "description": "开发与架构相关文章汇总。",
    "cover": "https://example.com/cover.jpg",
    "articleCount": 3,
    "createdAt": "2026-02-20",
    "updatedAt": "2026-02-25"
  }
]
```

字段说明同「16. 获取当前用户的专栏列表」单条。未传 `userId` 或该用户无专栏时返回空数组 `[]`。

---

### 17. 获取当前用户的专栏列表

**`GET /api/columns/me`**

需要认证。获取当前登录用户创建的所有专栏，用于个人页「我的专栏」展示。每条返回专栏名称、描述、封面、文章数、更新时间等。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "name": "技术笔记",
    "description": "开发与架构相关文章汇总。",
    "cover": "https://example.com/cover.jpg",
    "articleCount": 3,
    "createdAt": "2026-02-20",
    "updatedAt": "2026-02-25"
  }
]
```

| 字段          | 类型   | 说明           |
|---------------|--------|----------------|
| id            | number | 专栏 ID        |
| name          | string | 专栏名称       |
| description   | string | 专栏描述，可选 |
| cover         | string | 封面图 URL，可选 |
| articleCount  | number | 该专栏下文章数（类型为 BLOG 且 column_id 为本专栏） |
| createdAt     | string | 创建时间，格式 YYYY-MM-DD |
| updatedAt     | string | 更新时间，格式 YYYY-MM-DD |

---

### 18. 创建专栏

**`POST /api/columns`**

需要认证。创建新专栏，用于个人页「新建专栏」弹窗提交。

**Request Body**：

```json
{
  "name": "string",
  "description": "string",
  "cover": "string"
}
```

| 字段        | 类型   | 必填 | 说明         |
|-------------|--------|------|--------------|
| name        | string | 是   | 专栏名称，最长 128 字符 |
| description | string | 否   | 专栏描述，最长 512 字符 |
| cover       | string | 否   | 封面图 URL   |

**Response** `201 Created`：

返回新建的专栏对象，格式同「获取当前用户的专栏列表」单条（含 `id`、`articleCount` 为 0、`createdAt` / `updatedAt` 由服务端生成）。

---

### 19. 更新专栏

**`PATCH /api/columns/{id}`**

需要认证。修改专栏的名称、描述和/或封面，仅允许修改本人创建的专栏。

**路径参数**：

| 名称 | 类型   | 说明   |
|------|--------|--------|
| id   | number | 专栏 ID |

**Request Body**（均为可选，传则更新）：

```json
{
  "name": "string",
  "description": "string",
  "cover": "string"
}
```

| 字段        | 类型   | 必填 | 说明         |
|-------------|--------|------|--------------|
| name        | string | 否   | 专栏名称，最长 128 字符 |
| description | string | 否   | 专栏描述，最长 512 字符 |
| cover       | string | 否   | 封面图 URL   |

**Response** `200 OK`：返回更新后的专栏对象，格式同「获取当前用户的专栏列表」单条。

**错误**：专栏不存在或非当前用户创建，返回 `404 Not Found`。若 `name` 传空字符串或仅空白，返回 `400 Bad Request`。

---

### 20. 删除专栏

**`DELETE /api/columns/{id}`**

需要认证。删除指定专栏；仅允许删除本人创建的专栏。删除后，该专栏下文章的归属（column_id）会被置空，文章本身保留。

**路径参数**：

| 名称 | 类型   | 说明   |
|------|--------|--------|
| id   | number | 专栏 ID |

**Response** `204 No Content`：无响应体。

**错误**：专栏不存在或非当前用户创建，返回 `404 Not Found`。

---

## 知识库相关（content-service）

知识库用于收录文章，支持私有/公开可见性；用户可创建、编辑、删除自己的知识库，可订阅他人的公开知识库。默认知识库为前端占位（id 固定或由后端标记），逻辑同普通知识库。

**请求头**：需认证的接口均需携带网关下发的用户 ID（如 `X-User-Id`）。

### 知识库 1. 获取当前用户的知识库列表（我的知识库）

**`GET /api/knowledge-bases/me`**

需要认证。获取当前登录用户创建的所有知识库，用于知识库页「我的知识库」列表。不包含默认占位，默认知识库由前端或后端约定（如名称「默认知识库」或 isDefault）。

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "name": "默认知识库",
    "cover": "https://example.com/cover.jpg",
    "description": "默认创建的知识库，可在此收录文章与文件。",
    "visibility": "PRIVATE",
    "ownerId": 1,
    "ownerName": "用户昵称",
    "ownerAvatar": "https://example.com/avatar.jpg",
    "subCount": 0,
    "contentCount": 2,
    "createdAt": "2026-02-27",
    "updatedAt": "2026-02-27"
  }
]
```

| 字段         | 类型   | 说明 |
|--------------|--------|------|
| id           | number | 知识库 ID |
| name         | string | 知识库名称 |
| cover        | string | 封面图 URL，可选 |
| description  | string | 简介，可选 |
| visibility   | string | PRIVATE-私有 / PUBLIC-公开 |
| ownerId      | number | 创建者用户 ID |
| ownerName    | string | 创建者昵称，可选 |
| ownerAvatar  | string | 创建者头像 URL，可选 |
| subCount     | number | 订阅数 |
| contentCount | number | 收录文章数 |
| createdAt    | string | 创建时间，格式 YYYY-MM-DD |
| updatedAt    | string | 更新时间，格式 YYYY-MM-DD |

---

### 知识库 2. 热门知识库列表（公开，按订阅数排序，可搜索）

**`GET /api/knowledge-bases/popular`**

需要认证。获取公开（PUBLIC）知识库列表，按订阅数（subCount）降序，用于「热门知识库」右侧面板。支持按名称、简介模糊搜索。

**Query 参数**：

| 参数     | 类型   | 必填 | 说明           |
|----------|--------|------|----------------|
| page     | number | 否   | 页码，从 1 开始，默认 1 |
| pageSize | number | 否   | 每页条数，默认 20      |
| q        | string | 否   | 搜索关键词；对名称、简介模糊匹配，不传则不过滤 |

**Response** `200 OK`：

```json
{
  "list": [ { "id": 1, "name": "知识库名", "cover": "", "description": "", "visibility": "PUBLIC", "ownerId": 1, "ownerName": "", "ownerAvatar": "", "subCount": 10, "contentCount": 5, "createdAt": "", "updatedAt": "" } ],
  "total": 100
}
```

返回格式与「知识库 1」单条一致；仅返回 visibility=PUBLIC 的知识库。

---

### 知识库 3. 获取当前用户订阅的知识库列表（我的订阅）

**`GET /api/knowledge-bases/subscribed`**

需要认证。获取当前用户已订阅的知识库列表，用于知识库页「我的订阅」展示。

**Response** `200 OK`：返回格式与「知识库 1」单条一致，为知识库对象数组。按订阅时间倒序。

---

### 知识库 3. 获取知识库详情

**`GET /api/knowledge-bases/{id}`**

需要认证（公开知识库也可对未登录有限展示，具体以网关为准）。获取指定知识库的详情；私有知识库仅创建者可见，公开知识库所有人可见。返回字段同「知识库 1」单条，用于详情边栏展示。

**路径参数**：`id` 知识库 ID。

**Response** `200 OK`：单个知识库对象（含 ownerName、ownerAvatar、subCount、contentCount）。

**错误**：知识库不存在或无权查看（私有且非本人）返回 `404 Not Found`。

---

### 知识库 4. 分页获取知识库收录的文章列表

**`GET /api/knowledge-bases/{id}/contents`**

需要认证。获取指定知识库内收录的文章列表（摘要信息），用于详情边栏「收录的文章」。仅当知识库对当前用户可见时可调用（本人或公开）。

**路径参数**：`id` 知识库 ID。

**Query 参数**：

| 参数     | 类型   | 必填 | 说明           |
|----------|--------|------|----------------|
| page     | number | 否   | 页码，从 1 开始，默认 1 |
| pageSize | number | 否   | 每页条数，默认 10      |

**Response** `200 OK`：

```json
{
  "list": [
    {
      "id": 1,
      "title": "文章标题",
      "summary": "摘要",
      "cover": "https://example.com/cover.jpg"
    }
  ],
  "total": 5
}
```

| 字段        | 类型   | 说明     |
|-------------|--------|----------|
| list        | array  | 当前页数据 |
| list[].id   | number | 内容 ID  |
| list[].title| string | 标题     |
| list[].summary | string | 摘要，可选 |
| list[].cover  | string | 封面 URL，可选 |
| total      | number | 收录总数 |

---

### 知识库 5. 创建知识库

**`POST /api/knowledge-bases`**

需要认证。创建新知识库。

**Request Body**：

```json
{
  "name": "string",
  "description": "string",
  "cover": "string",
  "visibility": "PRIVATE"
}
```

| 字段        | 类型   | 必填 | 说明 |
|-------------|--------|------|------|
| name        | string | 是   | 知识库名称，最长 128 字符 |
| description | string | 否   | 简介，最长 512 字符 |
| cover       | string | 否   | 封面图 URL |
| visibility  | string | 否   | PRIVATE / PUBLIC，默认 PRIVATE |

**Response** `201 Created`：返回新建的知识库对象，格式同「知识库 1」单条（含 id、contentCount 为 0、subCount 为 0）。

**错误**：`name` 为空或仅空白时返回 `400 Bad Request`。

---

### 知识库 6. 更新知识库

**`PATCH /api/knowledge-bases/{id}`**

需要认证。修改知识库的名称、简介、封面、可见性；仅允许修改本人创建的知识库。

**路径参数**：`id` 知识库 ID。

**Request Body**（均为可选，传则更新）：

```json
{
  "name": "string",
  "description": "string",
  "cover": "string",
  "visibility": "PRIVATE"
}
```

| 字段        | 类型   | 必填 | 说明 |
|-------------|--------|------|------|
| name        | string | 否   | 知识库名称，最长 128 字符 |
| description | string | 否   | 简介 |
| cover       | string | 否   | 封面图 URL |
| visibility  | string | 否   | PRIVATE / PUBLIC |

**Response** `200 OK`：返回更新后的知识库对象。

**错误**：知识库不存在或非当前用户创建返回 `404 Not Found`；`name` 传空字符串返回 `400 Bad Request`。

---

### 知识库 7. 删除知识库

**`DELETE /api/knowledge-bases/{id}`**

需要认证。删除指定知识库；仅允许删除本人创建的知识库。删除后，该知识库的收录关系与订阅关系会随表外键一并删除。

**路径参数**：`id` 知识库 ID。

**Response** `204 No Content`

**错误**：知识库不存在或非当前用户创建返回 `404 Not Found`。

---

### 知识库 8. 添加文章到知识库（收录）

**`POST /api/knowledge-bases/{id}/contents`**

需要认证。将指定文章（content）收录到知识库；仅允许对本人创建的知识库操作，且仅可收录本人已发布的博客文章。

**路径参数**：`id` 知识库 ID。

**Request Body**：

```json
{
  "contentId": 1
}
```

| 字段      | 类型   | 必填 | 说明     |
|-----------|--------|------|----------|
| contentId | number | 是   | 内容 ID  |

**Response** `201 Created`：无响应体或返回 `{}`。

**错误**：知识库或文章不存在、非本人、文章未发布或非博客类型返回 `404`/`400`；已收录则幂等或返回 `400`「已收录」。

---

### 知识库 8.1 在知识库中新建文件

**`POST /api/knowledge-bases/{id}/contents/new-file`**

需要认证。在当前知识库中新建一条草稿内容（文件），并自动收录到该知识库；仅允许对本人创建的知识库操作。新建内容的 **type 为 KNOWLEDGE**（知识库类型），非 BLOG。用于「添加文件」：前端可传初始标题（如「未命名」「未命名 (1)」），不传则后端使用「未命名」。

**路径参数**：`id` 知识库 ID。

**Request Body**（可选）：

```json
{
  "title": "未命名 (1)"
}
```

| 字段  | 类型   | 必填 | 说明           |
|-------|--------|------|----------------|
| title | string | 否   | 初始标题，默认「未命名」 |

**Response** `201 Created`：

```json
{
  "id": 123,
  "title": "未命名 (1)",
  "summary": null,
  "cover": null
}
```

返回格式同「知识库 4」列表项（KnowledgeBaseContentItemVO）。

**错误**：知识库不存在或非当前用户返回 `404`。

---

### 知识库 9. 从知识库移除文章

**`DELETE /api/knowledge-bases/{id}/contents?contentId={contentId}`**

需要认证。从知识库中移除指定文章；仅允许对本人创建的知识库操作。

**路径参数**：`id` 知识库 ID。**Query 参数**：`contentId` 内容 ID。

**Response** `204 No Content`

**错误**：知识库不存在或非当前用户返回 `404`；该文章不在本知识库中返回 `400`。

---

### 知识库 10. 订阅知识库

**`POST /api/knowledge-bases/{id}/subscribe`**

需要认证。订阅指定知识库；仅可订阅公开（PUBLIC）知识库，不可订阅本人创建的知识库。重复调用视为已订阅（幂等）。

**路径参数**：`id` 知识库 ID。

**Request Body**：无（或 `{}`）。

**Response** `204 No Content`

**错误**：知识库不存在返回 `404`；私有知识库或本人知识库返回 `400`。

---

### 知识库 11. 取消订阅知识库

**`DELETE /api/knowledge-bases/{id}/subscribe`**

需要认证。取消对指定知识库的订阅。

**路径参数**：`id` 知识库 ID。

**Response** `204 No Content`

**错误**：知识库不存在返回 `404`。

---

## 博客机器人相关（ai-service）

创作者中心「博客机器人」：当前用户创建的机器人列表，支持新建（名称、头像、发文风格、主标签、默认摘要风格、字数偏好）。

### 21. 获取当前用户的博客机器人列表

**`GET /api/blog-bots/me`**

需要认证。获取当前登录用户创建的所有博客机器人，用于创作者中心「博客机器人」列表展示。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "name": "技术博客助手",
    "avatar": "https://example.com/avatar.jpg",
    "style": "professional",
    "mainTagId": 3,
    "mainTagName": "大模型与对齐（LLM/RLHF/DPO）",
    "summaryStyle": "concise",
    "wordCountPreference": "medium",
    "createdAt": "2026-02-21",
    "updatedAt": "2026-02-21"
  }
]
```

| 字段               | 类型   | 说明 |
|--------------------|--------|------|
| id                 | number | 机器人 ID |
| name               | string | 机器人名称 |
| avatar             | string | 头像 URL，可选 |
| style              | string | 发文风格：professional / casual / technical / narrative |
| mainTagId          | number | 主标签 ID，可选 |
| mainTagName        | string | 主标签名称，可选 |
| summaryStyle       | string | 默认摘要风格：concise / detailed / quote |
| wordCountPreference| string | 字数偏好：short / medium / long |
| createdAt          | string | 创建时间，格式 YYYY-MM-DD |
| updatedAt          | string | 更新时间，格式 YYYY-MM-DD |

---

### 22. 创建博客机器人

**`POST /api/blog-bots`**

需要认证。创建新博客机器人，用于创作者中心「新建机器人」弹窗提交。

**Request Body**：

```json
{
  "name": "string",
  "avatar": "string",
  "style": "professional",
  "mainTagId": 0,
  "summaryStyle": "concise",
  "wordCountPreference": "medium"
}
```

| 字段                | 类型   | 必填 | 说明 |
|---------------------|--------|------|------|
| name                | string | 是   | 机器人名称，最长 32 字符 |
| avatar              | string | 否   | 头像 URL |
| style               | string | 是   | 发文风格：professional / casual / technical / narrative |
| mainTagId           | number | 否   | 主标签 ID |
| summaryStyle        | string | 是   | 默认摘要风格：concise / detailed / quote |
| wordCountPreference | string | 是   | 字数偏好：short / medium / long |

**Response** `201 Created`：

返回新建的博客机器人对象，格式同「获取当前用户的博客机器人列表」单条（含 `id`、`mainTagName` 由服务端根据 `mainTagId` 回填、`createdAt` / `updatedAt` 由服务端生成）。

---

### 23. 删除博客机器人

**`DELETE /api/blog-bots/{id}`**

需要认证。删除当前用户指定的博客机器人；仅能删除自己创建的机器人。

**路径参数**：`id` — 机器人 ID。

**Response** `204 No Content`：删除成功。

**Response** `404 Not Found`：机器人不存在或不属于当前用户。

---

## 关注相关（interaction-service）

个人页右侧展示当前用户的「关注了」与「关注者」数量，由 interaction-service 基于 `follow` 表统计。

### 24. 获取当前用户关注统计

**`GET /api/follow/me`**

需要认证。获取当前登录用户的关注数（我关注了多少人）与被关注数（多少人关注了我），用于个人页右侧「关注了」「关注者」展示。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Response** `200 OK`：

```json
{
  "followingCount": 4,
  "followerCount": 1
}
```

| 字段            | 类型   | 说明                         |
|-----------------|--------|------------------------------|
| followingCount  | number | 关注了：当前用户关注的人数（follow 表中 follower_id = 当前用户） |
| followerCount   | number | 关注者：关注当前用户的人数（follow 表中 followee_id = 当前用户） |

---

## 创作者中心统计

创作者中心首页需展示：总阅读量、总点赞量、粉丝数、收藏数，及每项对应的「昨日增长」。总阅读量/总点赞量/收藏数由当前用户发布的内容汇总得到，粉丝数由关注表统计；昨日增长为昨日 0 点至今日 0 点（服务器时区）的新增数。

### 25. 获取当前用户内容统计（总阅读/总点赞/收藏及昨日增长）

**`GET /api/contents/me/stats`**

需要认证。获取当前用户作为创作者的内容汇总统计，用于创作者中心数据卡片。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Response** `200 OK`：

```json
{
  "totalViewCount": 10334,
  "totalLikeCount": 128,
  "totalCollectionCount": 151,
  "yesterdayViewDelta": 0,
  "yesterdayLikeDelta": 2,
  "yesterdayCollectionDelta": 1
}
```

| 字段                    | 类型   | 说明                                                         |
|-------------------------|--------|--------------------------------------------------------------|
| totalViewCount          | number | 总阅读量：当前用户所有内容 view_count 之和                   |
| totalLikeCount          | number | 总点赞量：当前用户所有内容 like_count 之和                   |
| totalCollectionCount    | number | 收藏数：当前用户所有内容 collection_count 之和               |
| yesterdayViewDelta      | number | 昨日阅读增长。无按日浏览日志时固定为 0，前端可展示「昨日无变化」 |
| yesterdayLikeDelta      | number | 昨日点赞增长：昨日新增的 content_like 中，content 属于当前用户的数量 |
| yesterdayCollectionDelta| number | 昨日收藏增长：昨日新增的 content_collection 中，content 属于当前用户的数量 |

---

### 25.1 获取创作者数据分析（多维聚合）

**`GET /api/contents/me/analytics`**

需要认证。对当前用户全部博客内容做多维聚合：总览指标、按日趋势、标签洞察、正文长度分桶、综合分 Top 内容、创作时间热力（按小时 / 星期）。用于创作者中心「数据分析」大屏。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Query 参数**：

| 参数 | 类型   | 必填 | 说明 |
|------|--------|------|------|
| days | number | 否   | 趋势统计窗口天数，默认 `30`；服务端限制在 **7～90**（小于 7 按 7，大于 90 按 90） |

**Response** `200 OK`：

```json
{
  "overview": {
    "totalContents": 42,
    "publishedContents": 38,
    "draftContents": 4,
    "totalViews": 12050,
    "totalLikes": 320,
    "totalCollections": 180,
    "totalComments": 95,
    "totalEngagement": 595,
    "avgViewsPerPublished": 317.1,
    "avgEngagementPerPublished": 15.7,
    "publishRate": 0.9,
    "followers": 120,
    "following": 45
  },
  "trend": [
    {
      "date": "2025-03-15",
      "publishedCount": 1,
      "views": 1200,
      "likes": 10,
      "collections": 5,
      "comments": 2,
      "score": 45.2
    }
  ],
  "tagInsights": [
    {
      "tagId": 3,
      "tagName": "后端",
      "articleCount": 12,
      "views": 5000,
      "engagement": 200
    }
  ],
  "lengthDistribution": [
    { "bucket": "0-500", "count": 5, "ratio": 0.12 },
    { "bucket": "500-1500", "count": 10, "ratio": 0.24 },
    { "bucket": "1500-3000", "count": 15, "ratio": 0.36 },
    { "bucket": "3000+", "count": 12, "ratio": 0.29 }
  ],
  "topContents": [
    {
      "contentId": 101,
      "title": "示例标题",
      "publishedAt": "2025-03-10 14:00:00",
      "views": 2000,
      "engagement": 88,
      "score": 52.3
    }
  ],
  "heatmap": {
    "hourCounts": [0,0,0,0,0,1,2,5,8,12,10,6,4,3,2,1,0,0,0,0,0,0,0,0],
    "weekDayCounts": [5,8,6,7,9,4,3]
  }
}
```

| 字段路径 | 类型 | 说明 |
|----------|------|------|
| overview.totalContents | number | 博客条数（含草稿） |
| overview.publishedContents | number | 已发布篇数 |
| overview.draftContents | number | 草稿篇数 |
| overview.totalViews 等 | number | 全量内容的阅读/赞/收藏/评论/互动总和 |
| overview.avgViewsPerPublished | number | 均篇阅读（仅已发布） |
| overview.avgEngagementPerPublished | number | 均篇互动（赞+收藏+评论，仅已发布） |
| overview.publishRate | number | 已发布 / 总篇数，0～1 |
| overview.followers / following | number | 由 interaction-service `GET /api/follow/me` 汇总 |
| trend[] | array | 最近 `days` 天每日一行；`score` 为当日内容加权综合分（含 log(阅读) 等） |
| tagInsights[] | array | 按标签聚合，按 engagement 降序，最多 8 条 |
| lengthDistribution[] | array | 正文长度分桶及占比 |
| topContents[] | array | 已发布内容按综合分降序，最多 8 条 |
| heatmap.hourCounts | number[] | 长度 24，按创建时刻小时 0～23 计数 |
| heatmap.weekDayCounts | number[] | 长度 7，周一至周日（与 Java `DayOfWeek` 顺序一致） |

---

### 26. 获取当前用户关注统计（扩展：昨日粉丝增长）

**`GET /api/follow/me`**（在原有接口上扩展返回字段）

需要认证。获取当前用户的关注数、被关注数（粉丝数），及昨日新增粉丝数，用于创作者中心与个人页。

**请求头**：需携带网关下发的用户 ID（如 `X-User-Id`）。

**Response** `200 OK`：

```json
{
  "followingCount": 4,
  "followerCount": 46,
  "yesterdayFollowerDelta": 0
}
```

| 字段                   | 类型   | 说明                                                         |
|------------------------|--------|--------------------------------------------------------------|
| followingCount         | number | 关注了：当前用户关注的人数                                   |
| followerCount          | number | 粉丝数：关注当前用户的人数                                   |
| yesterdayFollowerDelta | number | 昨日粉丝增长：follow 表中 followee_id=当前用户且 created_at 为昨日 0 点至今日 0 点的记录数 |

---

## AI 相关（ai-service）

### 根据正文生成博客标题

**`POST /api/ai/title`**

需要认证。根据正文内容（Markdown 或纯文本）调用大模型生成建议标题，用于创作页「AI 生成标题」等场景。

**Request Body**:

```json
{
  "body": "string"
}
```

| 字段  | 类型   | 必填 | 说明                                   |
|-------|--------|------|----------------------------------------|
| body  | string | 是   | 正文内容；可为 Markdown，建议前 2000 字参与生成 |

**Response** `200 OK`:

```json
{
  "title": "string"
}
```

| 字段   | 类型   | 说明           |
|--------|--------|----------------|
| title  | string | 生成的标题建议 |

**错误**：若 `body` 为空或仅空白，返回 `400 Bad Request`；若 AI 服务不可用，返回 `502 Bad Gateway` 或 `503 Service Unavailable`。

---

### 根据正文生成文章摘要

**`POST /api/ai/summary`**

需要认证。根据正文内容（Markdown 或纯文本）调用大模型生成文章摘要，用于创作页「AI 提取摘要」。**摘要上限 100 个字符**，服务端会截断超出部分。

**Request Body**:

```json
{
  "body": "string"
}
```

| 字段  | 类型   | 必填 | 说明                                   |
|-------|--------|------|----------------------------------------|
| body  | string | 是   | 正文内容；可为 Markdown，建议前 2000 字参与生成 |

**Response** `200 OK`:

```json
{
  "summary": "string"
}
```

| 字段     | 类型   | 说明                                   |
|----------|--------|----------------------------------------|
| summary  | string | 生成的摘要，最多 100 个字符             |

**错误**：若 `body` 为空或仅空白，返回 `400 Bad Request`；若 AI 服务不可用，返回 `502 Bad Gateway` 或 `503 Service Unavailable`。

---

### 根据正文生成标签

**`POST /api/ai/tags`**

需要认证。根据正文内容（Markdown 或纯文本）调用大模型生成文章标签建议，用于创作页「AI 生成标签」。**最多返回 5 个标签名称**（不含主标签，可与主标签组合后一起提交保存）。

**Request Body**:

```json
{
  "body": "string"
}
```

| 字段  | 类型   | 必填 | 说明                                   |
|-------|--------|------|----------------------------------------|
| body  | string | 是   | 正文内容；可为 Markdown，建议前 2000 字参与生成 |

**Response** `200 OK`:

```json
{
  "tagNames": ["标签1", "标签2", "标签3"]
}
```

| 字段     | 类型     | 说明                           |
|----------|----------|--------------------------------|
| tagNames | string[] | 生成的标签名称列表，最多 5 个 |

**错误**：若 `body` 为空或仅空白，返回 `400 Bad Request`；若 AI 服务不可用，返回 `502 Bad Gateway` 或 `503 Service Unavailable`。

---

### 文生图（Z-Image，通用）

**`POST /api/ai/image`**

需要认证。根据前端传入的文本描述（prompt）调用阿里云 DashScope 文生图（Z-Image），生成图片后由 file-service 存入 MinIO，并返回可供前端使用的 MinIO 访问 URL。**通用生图接口**，创作页、头像/封面等场景均可复用。

**Request Body**:

```json
{
  "prompt": "string",
  "size": "1120*1440"
}
```

| 字段    | 类型   | 必填 | 说明 |
|---------|--------|------|------|
| prompt  | string | 是   | 图片描述文案，即发给文生图模型的提示词 |
| size    | string | 否   | 图片尺寸，默认 `1120*1440`；需符合模型支持的尺寸 |

**Response** `200 OK`:

```json
{
  "url": "/api/objects/ai/xxx.png?stream=1"
}
```

| 字段 | 类型   | 说明 |
|------|--------|------|
| url  | string | 图片在 MinIO 上的访问路径（相对路径），前端拼接网关或站点 base URL 后即可展示或存储 |

**说明**：服务端会调用阿里云 DashScope 文生图接口，将返回的图片拉取并上传至 file-service（MinIO），再将该 URL 返回给前端。

**错误**：若 `prompt` 为空或仅空白，返回 `400 Bad Request`；若 DashScope 或 file-service 不可用，返回 `502 Bad Gateway` 或 `503 Service Unavailable`。

---

### 根据正文生成封面图

**`POST /api/ai/cover`**

需要认证。根据正文内容先由大模型生成封面图描述，再调用文生图（Z-Image）生成图片并存入 MinIO，返回封面访问 URL。用于创作页「AI 生成封面」。生成图片长宽比固定为 **10:7**（与前端列表/卡片中博客封面展示比例一致，如 120×84、200×140）。

**Request Body**:

```json
{
  "body": "string"
}
```

| 字段  | 类型   | 必填 | 说明                                   |
|-------|--------|------|----------------------------------------|
| body  | string | 是   | 正文内容；建议前 2000 字参与生成封面描述 |

**Response** `200 OK`:

```json
{
  "url": "/api/objects/ai/xxx.png?stream=1"
}
```

| 字段 | 类型   | 说明 |
|------|--------|------|
| url  | string | 封面图在 MinIO 上的访问路径，前端可直接用作封面地址 |

**错误**：若 `body` 为空或仅空白，返回 `400 Bad Request`；若 AI 或 file-service 不可用，返回 `502`/`503`。

---

### 一键生成（正文、标题、封面、主标签）

**`POST /api/ai/one-click-generate`**

需要认证。根据所选博客机器人（bot）与用户输入的 prompt，依次生成：正文（Markdown）、标题、封面图 URL、主标签 ID。用于创作页「一键生成」：选择机器人、输入主题后一次性拉取并回填到编辑区。请求头需携带 `X-User-Id`（网关下发）；仅能使用当前用户自己创建的 bot。

**Request Body**:

```json
{
  "botId": 1,
  "prompt": "写一篇关于机器学习入门的知识梳理"
}
```

| 字段   | 类型   | 必填 | 说明 |
|--------|--------|------|------|
| botId  | number | 是   | 博客机器人 ID，须属于当前用户 |
| prompt | string | 是   | 主题或描述，如「写一篇关于…」 |

**Response** `200 OK`:

```json
{
  "body": "# 机器学习入门\n\n...",
  "title": "机器学习入门：从零开始的知识梳理",
  "coverUrl": "/api/objects/ai/xxx.png?stream=1",
  "mainTagId": 1
}
```

| 字段      | 类型   | 说明 |
|-----------|--------|------|
| body      | string | 生成的正文（Markdown），可直接写入编辑器 |
| title     | string | 生成的标题 |
| coverUrl  | string | 封面图 URL，可为 null（生成失败时） |
| mainTagId  | number | 机器人配置的主标签 ID，可为 null |

**错误**：`400` 参数缺失或无效；`404` bot 不存在或不属于当前用户；`502`/`503` AI 或下游服务不可用。

---

## 文件服务（file-service）内部接口

以下接口主要供其他微服务（如 ai-service）调用，前端一般不直接使用。

### 从 URL 拉取并存入 MinIO

**`POST /api/objects/from-url`**

将指定 URL 的图片下载并上传到 MinIO，返回对象元信息（含访问 URL）。用于 ai-service 文生图后将第三方（如 DashScope OSS）图片持久化到本站存储。

**Request Body**:

```json
{
  "url": "https://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/xxx.png?Expires=xxx",
  "prefix": "ai"
}
```

| 字段   | 类型   | 必填 | 说明 |
|--------|--------|------|------|
| url    | string | 是   | 图片完整 URL，当前仅允许阿里云 DashScope 结果域名 |
| prefix | string | 否   | MinIO 对象前缀，如 `ai`，默认 `ai` |

**Response** `201 Created`:

与 `POST /api/objects` 一致，返回 `ObjectMetaVO`（`key`、`url`、`size`、`contentType`），其中 `url` 为本站访问路径（如 `/api/objects/ai/xxx.png?stream=1`）。

---

## 审核中心（content-service）

### A1. 提交审核任务（业务服务内部调用）

**`POST /api/admin/moderation/tasks/submit`**

用于内容服务/互动服务/用户服务提交审核任务（统一写入 `moderation_task`）。  
管理员账号提交时返回 `skipped=true`（免审）。

**Request Body**:

```json
{
  "resourceType": "ARTICLE",
  "resourceId": 123,
  "ownerUserId": 1,
  "payloadSnapshot": "title=...\\nbody=..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| resourceType | string | 是 | `ARTICLE` / `KNOWLEDGE_DOC` / `COMMENT` / `USER_PROFILE` |
| resourceId | number | 是 | 业务资源 ID |
| ownerUserId | number | 是 | 提交者 UID |
| payloadSnapshot | string | 否 | 送审快照文本（供 AI/人工查看） |

**Response** `201 Created`:

```json
{
  "id": 10,
  "status": "NEEDS_HUMAN",
  "aiDecision": "NEEDS_HUMAN"
}
```

或：

```json
{
  "skipped": true,
  "reason": "admin_exempt"
}
```

### A2. 审核任务列表（管理员）

**`GET /api/admin/moderation/tasks`**

需要管理员登录。

**Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | number | 否 | 默认 1 |
| pageSize | number | 否 | 默认 20，最大 100 |
| resourceType | string | 否 | 同上四类 |
| status | string | 否 | `ALL` / `PENDING` / `NEEDS_HUMAN` / `APPROVED` / `REJECTED` |
| finishedOnly | boolean | 否 | 是否仅已结单 |

**Response** `200 OK`:

```json
{
  "records": [
    {
      "id": 10,
      "resourceType": "ARTICLE",
      "resourceId": 123,
      "ownerUserId": 1,
      "status": "NEEDS_HUMAN",
      "aiDecision": "NEEDS_HUMAN",
      "createdAt": "2026-03-17T10:30:00"
    }
  ],
  "total": 1
}
```

### A3. 审核统计（管理员首页）

**`GET /api/admin/moderation/stats`**

**Response** `200 OK`:

```json
{
  "pending": 12,
  "pendingHuman": 4,
  "todayFinished": 9,
  "rejected7d": 3
}
```

### A4. 人工审核

**`POST /api/admin/moderation/tasks/{id}/human-review`**

**Request Body**:

```json
{
  "decision": "APPROVE",
  "note": "内容合规，允许发布"
}
```

`decision` 仅支持 `APPROVE` / `REJECT`。

### A5. 重新执行 AI 审核

**`POST /api/admin/moderation/tasks/{id}/ai-review`**

重新调用 ai-service 的审核接口，刷新 AI 结论与任务状态。

---

## 收件箱（user-service）

### B1. 获取当前用户消息列表

**`GET /api/users/me/messages`**

需要认证。

**Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | number | 否 | 默认 1 |
| pageSize | number | 否 | 默认 20 |
| unreadOnly | boolean | 否 | `true` 仅未读 |

**Response** `200 OK`:

```json
{
  "records": [
    {
      "id": 1,
      "title": "博客审核通过",
      "body": "你的博客（ID=123）已通过审核。",
      "msgType": "AUDIT",
      "scene": "MODERATION_RESULT",
      "read": false,
      "createdAt": "2026-03-17T10:40:00"
    }
  ],
  "total": 1
}
```

### B2. 未读数量

**`GET /api/users/me/messages/unread-count`**

**Response**:

```json
{ "count": 3 }
```

### B3. 标记单条已读

**`PATCH /api/users/me/messages/{id}/read`** → `204 No Content`

### B4. 全部标记已读

**`PATCH /api/users/me/messages/read-all`** → `204 No Content`

---

## AI 审核接口（ai-service）

### C1. 审核预判

**`POST /api/ai/moderation/review`**

供业务服务调用，返回 `PASS` / `REJECT` / `NEEDS_HUMAN`。

**Request Body**:

```json
{
  "resourceType": "ARTICLE",
  "content": "待审核文本快照"
}
```

**Response**:

```json
{
  "decision": "NEEDS_HUMAN",
  "reason": "命中人工复核关键词: 政治",
  "score": 0.62
}
```

---

## 错误响应

| HTTP 状态码 | 说明     |
|-------------|----------|
| 400         | 请求参数错误 |
| 401         | 未认证或 Token 失效 |
| 404         | 资源不存在   |
| 409         | 用户名已存在（注册） |
| 500         | 服务端错误   |

**错误响应体**:

```json
{
  "message": "string",
  "code": "string"
}
```
