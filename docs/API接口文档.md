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
  "role": "string"
}
```

---

### 4. 注销会话（登出）

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
