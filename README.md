# INK 短信网关

基于 Spring Cloud + Vue 3 的现代化短信网关系统，提供统一的 API 网关路由、JWT 认证与用户管理能力。

## 系统架构

```
┌─────────────┐     ┌──────────────────────┐     ┌───────────────────┐
│   Frontend   │────▶│  Gateway Service     │────▶│  User Service     │
│  Vue 3 SPA   │     │  Spring Cloud Gateway│     │  RESTful 微服务    │
│  :3000       │     │  :8080               │     │  :8081            │
└─────────────┘     └──────────┬───────────┘     └────────┬──────────┘
                               │                          │
                     ┌─────────┴─────────┐      ┌─────────┴─────────┐
                     │  Nacos (8888)     │      │  MySQL (3306)     │
                     │  服务注册与发现     │      │  数据持久化         │
                     └───────────────────┘      └───────────────────┘
                               │                          │
                     ┌─────────┴─────────┐      ┌─────────┴─────────┐
                     │  Redis            │      │  Redis            │
                     │  DB 0 - 网关缓存   │      │  DB 1 - 用户缓存   │
                     └───────────────────┘      └───────────────────┘
```

## 项目结构

```
ink-sms-gateway/
├── backend/
│   ├── pom.xml                          # Maven 根 POM（版本统一管理）
│   ├── common/                          # 公共模块
│   │   └── src/main/java/com/ink/common/
│   │       ├── config/                  # Redis 配置
│   │       ├── constant/                # 常量（API/JWT/Redis/User）
│   │       ├── exception/               # 业务异常 & 全局异常处理
│   │       └── utils/                   # 工具类（JWT/Redis/Result）
│   ├── gateway/                         # 网关服务（:8080）
│   │   └── src/main/java/com/ink/gateway/
│   │       ├── config/                  # 网关配置（CORS 等）
│   │       ├── controller/              # 网关状态 & 健康检查
│   │       └── filter/                  # JWT 全局过滤器
│   └── user-service/                    # 用户服务（:8081）
│       └── src/main/
│           ├── java/com/ink/user/
│           │   ├── config/              # Security 配置
│           │   ├── controller/          # 用户 API 控制器
│           │   ├── dto/request/         # 请求 DTO
│           │   ├── entity/              # 用户实体
│           │   ├── mapper/              # MyBatis Mapper
│           │   ├── service/             # 业务逻辑
│           │   └── util/                # 工具类
│           └── resources/
│               ├── db/init.sql          # 数据库初始化脚本
│               └── mapper/              # MyBatis XML 映射
├── front/                               # 前端 SPA
│   └── src/
│       ├── api/                         # API 请求封装
│       ├── components/                  # 公共组件（AppLayout）
│       ├── router/                      # 路由配置 & 守卫
│       ├── stores/                      # Pinia 状态管理
│       ├── types/                       # TypeScript 类型定义
│       ├── utils/                       # Axios 封装
│       └── views/                       # 页面组件
```

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.0 |
| **微服务** | Spring Cloud | 2023.0.0 |
| **服务注册** | Spring Cloud Alibaba (Nacos) | 2023.0.3.4 |
| **网关** | Spring Cloud Gateway | (随 Spring Cloud) |
| **ORM** | MyBatis | 3.5.15 |
| **数据库** | MySQL | 8.0.33 |
| **缓存** | Spring Data Redis (Lettuce) | 3.2.1 |
| **认证** | JJWT | 0.12.3 |
| **API 文档** | Knife4j (OpenAPI 3) | 4.4.0 |
| **工具** | Hutool / Lombok | 5.8.25 / 1.18.40 |
| **前端框架** | Vue 3 + TypeScript | 3.4.0 / 5.3.3 |
| **构建** | Vite | 5.0.8 |
| **UI** | Element Plus | 2.4.4 |
| **状态管理** | Pinia | 2.1.7 |
| **HTTP** | Axios | 1.6.2 |

## 环境要求

- **JDK** 17+
- **Node.js** 18+
- **MySQL** 8.0+
- **Redis** 6.0+
- **Nacos** 2.2+（需 standalone 模式启动）
- **Maven** 3.6+

## 快速开始

### 1. 克隆项目

```bash
git clone <repo-url>
cd ink-sms-gateway
```

### 2. 初始化数据库

```bash
mysql -u root -p < backend/user-service/src/main/resources/db/init.sql
```

这会创建 `ink_sms_gateway` 数据库及 `users` 表，并插入两条测试数据：
- `admin` / 密码: `admin123`
- `test` / 密码: `test123`

### 3. 启动中间件

```bash
# 启动 Redis（默认端口 6379）
redis-server

# 启动 Nacos（standalone 模式，端口 8888）
cd <nacos-home>/bin
startup.cmd -m standalone
```

> **注意**: Nacos 3.x 需要 JVM 参数 `-Dnacos.server.grpc.port.offset=1000` 以兼容 Spring Cloud Alibaba 2023.x。

### 4. 启动后端服务

```bash
# 编译公共模块
cd backend
mvn clean install -pl common

# 启动网关服务（:8080）
cd gateway
mvn spring-boot:run

# 启动用户服务（:8081）
cd ../user-service
mvn spring-boot:run
```

### 5. 启动前端

```bash
cd front
npm install
npm run dev
```

访问 **http://localhost:3000**

## API 接口

所有接口统一前缀 `/api`，由网关路由分发。认证接口需在请求头携带 `Authorization: Bearer <token>`。

### 用户服务

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/user/register` | 用户注册 | 否 |
| `POST` | `/api/user/login` | 用户登录 | 否 |
| `POST` | `/api/user/logout` | 用户登出 | 是 |
| `GET` | `/api/user/info` | 获取当前用户信息 | 是 |
| `PUT` | `/api/user/info` | 更新当前用户信息 | 是 |
| `PUT` | `/api/user/password` | 修改密码 | 是 |
| `GET` | `/api/user/list?page=1&size=10` | 获取用户列表（分页） | 是 |
| `GET` | `/api/user/count` | 获取用户总数 | 是 |
| `GET` | `/api/user/username/{username}` | 按用户名查询 | 是 |
| `PUT` | `/api/user/{userId}/disable` | 禁用用户 | 是 |
| `PUT` | `/api/user/{userId}/enable` | 启用用户 | 是 |

### 网关服务

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/gateway/status` | 网关状态 | 否 |
| `GET` | `/api/gateway/health` | 健康检查 | 否 |

### 认证机制

网关通过 `JwtAuthenticationFilter`（GlobalFilter）统一拦截：
- **白名单路径**：`/api/user/login`、`/api/user/register`、Swagger、Actuator
- **认证流程**：请求头 `Authorization: Bearer <token>` → 解析 JWT → 注入 `X-User-Id` / `X-Username` 到下游请求头
- **密码加密**：BCrypt（Spring Security Crypto）

## 配置说明

### 关键配置项

| 配置项 | 文件 | 说明 |
|--------|------|------|
| `server.port` | 各服务 `application.yml` | 网关 8080，用户服务 8081 |
| `spring.cloud.nacos.discovery.server-addr` | 各服务 `application.yml` | Nacos 地址，当前 `localhost:8888` |
| `spring.datasource.*` | user-service `application.yml` | MySQL 连接配置 |
| `spring.data.redis.*` | 各服务 `application.yml` | Redis 连接（网关 DB0，用户服务 DB1） |
| `jwt.secret` | gateway `application.yml` | JWT 签名密钥（≥256 bit） |
| `mybatis.mapper-locations` | user-service `application.yml` | MyBatis XML 映射路径 |

### 数据库设计

**users 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `username` | VARCHAR(20) | 用户名（唯一） |
| `password` | VARCHAR(100) | BCrypt 加密密码 |
| `email` | VARCHAR(100) | 邮箱（唯一） |
| `phone` | VARCHAR(11) | 手机号（唯一） |
| `nickname` | VARCHAR(50) | 昵称 |
| `avatar` | VARCHAR(255) | 头像 URL |
| `status` | TINYINT | 0=禁用，1=正常 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |
| `last_login_time` | DATETIME | 最后登录时间 |
| `last_login_ip` | VARCHAR(45) | 最后登录 IP |

## 前端页面

| 路由 | 页面 | 说明 |
|------|------|------|
| `/` | Home | 首页 |
| `/login` | Login | 登录页 |
| `/register` | Register | 注册页 |
| `/dashboard` | Dashboard | 仪表盘（需登录） |
| `/profile` | Profile | 个人中心（需登录） |
| `/users` | Users | 用户管理（需登录） |

路由守卫：`meta.requiresAuth` 标记的页面未登录时自动跳转 `/login`；已登录访问 `/login` 自动跳转 `/dashboard`。

## 开发指南

### 后端

```bash
# 编译全部模块
cd backend
mvn clean install

# 单独编译某模块
mvn clean install -pl common
```

- 公共模块 `common` 被 `gateway` 和 `user-service` 依赖，修改后需先 `mvn install`
- Knife4j API 文档地址：`http://localhost:8080/doc.html`（网关）、`http://localhost:8081/doc.html`（用户服务）
- Actuator 端点：`/actuator/health`、`/actuator/info`、`/actuator/metrics`

### 前端

```bash
npm run dev       # 开发服务器
npm run build     # 生产构建
npm run lint      # ESLint 检查
npm run format    # Prettier 格式化
```

## 已知事项

- Nacos 需以 **standalone 模式**启动，集群模式需配置数据库
- Nacos 3.x 与 Spring Cloud Alibaba 2023.x 的 gRPC 端口偏移需通过 JVM 参数 `-Dnacos.server.grpc.port.offset=1000` 对齐
- 网关基于 WebFlux，**不可**引入 `knife4j` 或 `spring-boot-starter-web` 等 Servlet 依赖
- JWT 密钥长度需 ≥ 256 bit（32 字节），否则 HMAC-SHA 签名会报错