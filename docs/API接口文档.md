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
| sortBy     | string | 否   | 排序字段：time-按发布时间 / likes-按点赞量 / views-按浏览量，默认 time |
| order      | string | 否   | 排序方向：asc-升序 / desc-降序，默认 desc |

**Response** `200 OK`:

```json
{
  "list": [
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
| list[].viewCount      | number | 浏览数 |
| list[].likeCount      | number | 点赞数 |
| list[].collectionCount| number | 收藏数 |
| list[].createdAt| string | 创建时间，格式 YYYY-MM-DD 或 ISO 8601 |
| total           | number | 总条数（符合条件的内容总数） |

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
