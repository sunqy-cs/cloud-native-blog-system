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

### 5. 注销会话（登出）

**`DELETE /api/sessions/current`**

需要认证。使当前 Token 失效。

**Response** `204 No Content`

> 前端也可仅清除本地 Token 完成登出，无需调用此接口。

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
