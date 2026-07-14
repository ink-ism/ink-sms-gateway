# API 接口文档

## 概述

所有 API 统一前缀 `/api`，由网关路由分发。响应格式统一为 `Result<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1720000000000
}
```

**认证方式**：需要认证的接口必须在请求头中携带 `Authorization: Bearer <token>`。

---

## 用户服务接口

### POST /api/user/register — 用户注册

**认证**：否

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| `username` | string | 是 | 3-20 字符 |
| `password` | string | 是 | 6-20 字符 |
| `email` | string | 是 | 合法邮箱格式 |
| `phone` | string | 是 | 11 位手机号 |

**请求示例**：

```json
{
  "username": "newuser",
  "password": "123456",
  "email": "newuser@example.com",
  "phone": "13900139000"
}
```

**成功响应**：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": null,
  "timestamp": 1720000000000
}
```

**业务异常**：
- `用户名已存在`
- `邮箱已被注册`
- `手机号已被注册`

---

### POST /api/user/login — 用户登录

**认证**：否

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| `username` | string | 是 | 非空 |
| `password` | string | 是 | 非空 |

**请求示例**：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**成功响应**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "nickname": "管理员",
      "avatar": null,
      "loginTime": "2025-01-01T12:00:00"
    }
  },
  "timestamp": 1720000000000
}
```

**业务异常**：
- `用户名或密码错误`
- `账号已被禁用`

---

### POST /api/user/logout — 用户登出

**认证**：是（`X-User-Id` 由网关注入）

**请求体**：无

**成功响应**：

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": 1720000000000
}
```

---

### GET /api/user/info — 获取当前用户信息

**认证**：是

**请求头**：`Authorization: Bearer <token>`（网关解析后注入 `X-User-Id`）

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "password": null,
    "email": "admin@example.com",
    "phone": "13800138000",
    "nickname": "管理员",
    "avatar": null,
    "status": 1,
    "createTime": "2025-01-01T00:00:00",
    "updateTime": "2025-01-01T12:00:00",
    "lastLoginTime": "2025-01-01T12:00:00",
    "lastLoginIp": "127.0.0.1"
  },
  "timestamp": 1720000000000
}
```

> 注意：`password` 字段会被 `maskPassword()` 置为 null。

---

### PUT /api/user/info — 更新用户信息

**认证**：是

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 否 | 新用户名 |
| `email` | string | 否 | 新邮箱 |
| `phone` | string | 否 | 新手机号 |
| `nickname` | string | 否 | 昵称 |
| `avatar` | string | 否 | 头像 URL |

**业务异常**：
- `用户不存在`
- `用户名已被使用`
- `邮箱已被使用`
- `手机号已被使用`

---

### PUT /api/user/password — 修改密码

**认证**：是

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| `oldPassword` | string | 是 | 非空 |
| `newPassword` | string | 是 | 6-20 字符 |

**业务异常**：
- `用户不存在`
- `原密码错误`

> 密码修改成功后，Redis 中的 Token 会被清除，需要重新登录。

---

### GET /api/user/list — 获取用户列表

**认证**：是

**查询参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页大小 |

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    { "id": 1, "username": "admin", "password": null, ... },
    { "id": 2, "username": "test", "password": null, ... }
  ],
  "timestamp": 1720000000000
}
```

---

### GET /api/user/count — 获取用户总数

**认证**：是

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 42,
  "timestamp": 1720000000000
}
```

---

### GET /api/user/username/{username} — 按用户名查询

**认证**：是

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `username` | string | 用户名 |

**业务异常**：`用户不存在`

---

### PUT /api/user/{userId}/disable — 禁用用户

**认证**：是

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `userId` | long | 用户 ID |

**副作用**：清除该用户的 Redis Token（强制下线）

---

### PUT /api/user/{userId}/enable — 启用用户

**认证**：是

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `userId` | long | 用户 ID |

---

## 网关服务接口

### GET /api/gateway/status — 网关状态

**认证**：否

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "gateway": "running",
    "port": "8001",
    "timestamp": 1720000000000
  },
  "timestamp": 1720000000000
}
```

---

### GET /api/gateway/health — 健康检查

**认证**：否

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "UP",
    "gateway": "gateway-service",
    "timestamp": 1720000000000
  },
  "timestamp": 1720000000000
}
```

---

## 管理服务接口

### POST /api/admin/login — 管理员登录

**认证**：否

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |

**成功响应**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "adminId": 1,
    "username": "admin",
    "token": "eyJhbGciOi...",
    "role": "SUPER_ADMIN"
  },
  "timestamp": 1720000000000
}
```

---

### POST /api/admin/logout — 管理员登出

**认证**：是（需 `X-Admin-Id` 请求头）

---

### GET /api/admin/info — 获取管理员信息

**认证**：是（需 `X-Admin-Id` 请求头）

---

### PUT /api/admin/info — 更新管理员信息

**认证**：是（需 `X-Admin-Id` 请求头）

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 否 | 用户名 |
| `email` | string | 否 | 邮箱 |
| `phone` | string | 否 | 手机号 |
| `nickname` | string | 否 | 昵称 |
| `avatar` | string | 否 | 头像 URL |
| `role` | string | 否 | 角色 |

---

### PUT /api/admin/password — 修改密码

**认证**：是（需 `X-Admin-Id` 请求头）

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `oldPassword` | string | 是 | 旧密码 |
| `newPassword` | string | 是 | 新密码（6-20 字符） |

---

### GET /api/admin/list — 获取管理员列表

**认证**：是

**查询参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页大小 |

---

### GET /api/admin/count — 获取管理员总数

**认证**：是

---

### PUT /api/admin/{adminId}/disable — 禁用管理员

**认证**：是

---

### PUT /api/admin/{adminId}/enable — 启用管理员

**认证**：是

---

### POST /api/admin/create — 创建管理员

**认证**：是（需 `X-Admin-Id` 请求头）

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 用户名（3-20 字符） |
| `password` | string | 是 | 密码（6-20 字符） |
| `email` | string | 否 | 邮箱 |
| `phone` | string | 否 | 手机号 |
| `role` | string | 否 | 角色 |

---

## 通道管理接口

### GET /api/admin/channel/list — 获取通道列表

**认证**：是

**查询参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页大小 |
| `keyword` | string | - | 搜索关键字 |

---

### GET /api/admin/channel/{id} — 获取通道详情

**认证**：是

---

### POST /api/admin/channel — 创建通道

**认证**：是

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `channelCode` | string | 是 | 通道编码 |
| `channelName` | string | 是 | 通道名称 |
| `host` | string | 是 | 服务器地址 |
| `port` | int | 是 | 服务器端口 |
| `spId` | string | 是 | SP 标识 |
| `sharedSecret` | string | 是 | 共享密钥 |

---

### PUT /api/admin/channel/{id} — 更新通道

**认证**：是

---

### DELETE /api/admin/channel/{id} — 删除通道

**认证**：是

---

### PUT /api/admin/channel/{id}/enable — 启用通道

**认证**：是

---

### PUT /api/admin/channel/{id}/disable — 禁用通道

**认证**：是

---

## 短信记录接口

### GET /api/admin/sms/down/list — 下行短信列表

**认证**：是

**查询参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页大小 |
| `keyword` | string | - | 搜索关键字（手机号/内容） |

---

### GET /api/admin/sms/up/list — 上行短信列表

**认证**：是

**查询参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页大小 |
| `keyword` | string | - | 搜索关键字（手机号/内容） |

---

## 短信服务接口

### POST /api/sms/send — 发送短信

**认证**：是

**请求体**（JSON）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 目标手机号（11 位） |
| `content` | string | 否 | 短信内容 |
| `srcId` | string | 否 | 源号码，默认 `10690000` |
| `serviceId` | string | 否 | 业务标识，默认 `0000000000` |
| `msgFmt` | int | 否 | 消息格式：0=ASCII, 8=UCS2（默认）, 15=GB2312 |

**请求示例**：

```json
{
  "phone": "13800138000",
  "content": "测试短信内容"
}
```

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "message": "短信已提交"
  },
  "timestamp": 1720000000000
}
```

**业务异常**：
- `CMPP 上游连接未就绪`
- `短信提交失败`

---

### GET /api/sms/status — 查询连接状态

**认证**：是

**成功响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "upstreamConnected": true,
    "activeSpCount": 0
  },
  "timestamp": 1720000000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `upstreamConnected` | boolean | CMPP 上游连接是否就绪 |
| `activeSpCount` | int | 当前活跃的 SP 连接数 |

---

## 错误响应

### 认证失败（401）

由网关 JWT 过滤器返回：

```json
{
  "code": 401,
  "message": "未提供有效的认证令牌",
  "data": null,
  "timestamp": 1720000000000
}
```

### 参数校验失败（400）

由 `GlobalExceptionHandler` 返回：

```json
{
  "code": 400,
  "message": "用户名不能为空, 密码长度必须在6-20个字符之间",
  "data": null,
  "timestamp": 1720000000000
}
```

### 业务异常（500 或自定义 code）

```json
{
  "code": 500,
  "message": "用户名已存在",
  "data": null,
  "timestamp": 1720000000000
}
```
