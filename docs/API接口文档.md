# API 接口文档（RESTful）

## 基础说明

- **Base URL**: `/api`
- **Content-Type**: `application/json`
- **认证方式**: 登录后在请求头携带 `Authorization: Bearer {token}`

---

## 认证相关

### 1. 创建会话（登录）

**`POST /api/sessions`**

创建登录会话，获取访问令牌。

**Request Body**:

```json
{
  "username": "string",
  "password": "string"
}
```

| 字段     | 类型   | 必填 | 说明   |
|----------|--------|------|--------|
| username | string | 是   | 用户名 |
| password | string | 是   | 密码   |

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

### 2. 创建用户（注册）

**`POST /api/users`**

创建新用户账号。

**Request Body**:

```json
{
  "username": "string",
  "password": "string"
}
```

| 字段     | 类型   | 必填 | 说明           |
|----------|--------|------|----------------|
| username | string | 是   | 用户名         |
| password | string | 是   | 密码，至少 6 位 |

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

### 3. 获取当前用户

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
  "wechatId": "string",
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
| wechatId   | string | 微信 ID，可选        |
| role       | string | 角色                 |
| createdAt  | string | 创建时间             |

---

### 4. 更新当前用户资料（编辑个人资料）

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
  "bio": "个人简介内容",
  "wechatId": "wxid_xxx"
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
| wechatId  | string | 否   | 微信 ID                  |

**Response** `200 OK`: 返回更新后的用户对象，格式同 `GET /api/users/me`。

---

## 内容/博客相关

### 5. 分页获取当前用户的博客列表（我的博客）

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

### 5.1 保存草稿

**`POST /api/contents/draft`**

需要认证。将当前编辑内容保存为草稿。**仅当正文（body）不为空时允许保存**；若标题（title）为空，服务端将标题存为「[无标题]」。

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
| body              | string   | 是   | 正文（Markdown）；为空时返回 400，不允许保存 |
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

### 5.2 获取编辑用内容详情

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

### 5.3 发布博客

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

### 6. 获取当前用户的收藏夹列表

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

### 7. 创建收藏夹

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

### 8. 更新收藏夹

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

### 9. 删除收藏夹

**`DELETE /api/collection-folders/{id}`**

需要认证。删除指定收藏夹；仅允许删除本人创建的收藏夹。**默认收藏夹不可删除**。该收藏夹下的收藏关系会随表外键一并删除。

**路径参数**:

| 参数 | 类型 | 说明     |
|------|------|----------|
| id   | number | 收藏夹 ID |

**Response** `204 No Content`

若收藏夹不存在或非当前用户创建，返回 `404 Not Found`。若为默认收藏夹，返回 `400`，报错信息为「不能删除默认收藏夹」。

---

### 10. 注销会话（登出）

**`DELETE /api/sessions/current`**

需要认证。使当前 Token 失效。

**Response** `204 No Content`

> 前端也可仅清除本地 Token 完成登出，无需调用此接口。

---

## 动态 / 赞同相关（interaction-service）

个人页「我的动态」需要展示「赞同了文章」与「发表了博客」的混合时间线。赞同数据由 interaction-service 提供，内容摘要由 content-service 提供。

### 11. 分页获取当前用户赞同的内容列表（我赞同的文章）

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

### 12. 按 ID 批量获取内容摘要（用于动态等）

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

### 13. 获取有评论的文章列表（评论管理左侧）

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

### 14. 获取某篇文章的评论列表（评论管理右侧）

**`GET /api/comments/list?contentId={contentId}`**（interaction-service）

需要认证。仅允许该文章作者调用。返回该文章下所有评论，热评在前、再按时间倒序。

**Query 参数**：`contentId` 内容 ID。

**Response** `200 OK`：

```json
[
  {
    "id": 1,
    "userId": 2,
    "userNickname": "用户2",
    "contentId": 1,
    "body": "评论内容",
    "parentId": null,
    "isHot": true,
    "createdAt": "2026-02-26 13:42",
    "isAuthor": false
  }
]
```

| 字段          | 类型    | 说明                         |
|---------------|---------|------------------------------|
| id            | number  | 评论 ID                      |
| userId        | number  | 评论者用户 ID                |
| userNickname  | string  | 评论者昵称（可后续接用户服务） |
| contentId     | number  | 所属内容 ID                  |
| body          | string  | 评论正文                     |
| parentId      | number  | 父评论 ID，回复时存在        |
| isHot         | boolean | 是否热评（作者推荐）          |
| createdAt     | string  | 评论时间                     |
| isAuthor      | boolean | 是否文章作者本人评论         |

---

### 15. 设置/取消热评

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

个人页「我的专栏」展示当前用户创建的专栏列表，支持新建专栏（名称、描述、封面）。

### 16. 获取当前用户的专栏列表

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

### 17. 创建专栏

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

### 18. 更新专栏

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

### 19. 删除专栏

**`DELETE /api/columns/{id}`**

需要认证。删除指定专栏；仅允许删除本人创建的专栏。删除后，该专栏下文章的归属（column_id）会被置空，文章本身保留。

**路径参数**：

| 名称 | 类型   | 说明   |
|------|--------|--------|
| id   | number | 专栏 ID |

**Response** `204 No Content`：无响应体。

**错误**：专栏不存在或非当前用户创建，返回 `404 Not Found`。

---

## 博客机器人相关（ai-service）

创作者中心「博客机器人」：当前用户创建的机器人列表，支持新建（名称、头像、发文风格、主标签、默认摘要风格、字数偏好）。

### 20. 获取当前用户的博客机器人列表

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

### 21. 创建博客机器人

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

### 22. 删除博客机器人

**`DELETE /api/blog-bots/{id}`**

需要认证。删除当前用户指定的博客机器人；仅能删除自己创建的机器人。

**路径参数**：`id` — 机器人 ID。

**Response** `204 No Content`：删除成功。

**Response** `404 Not Found`：机器人不存在或不属于当前用户。

---

## 关注相关（interaction-service）

个人页右侧展示当前用户的「关注了」与「关注者」数量，由 interaction-service 基于 `follow` 表统计。

### 23. 获取当前用户关注统计

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

### 24. 获取当前用户内容统计（总阅读/总点赞/收藏及昨日增长）

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

### 25. 获取当前用户关注统计（扩展：昨日粉丝增长）

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
