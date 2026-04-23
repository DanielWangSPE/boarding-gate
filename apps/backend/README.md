# boarding-gate-backend

Boarding Gate 后端服务（Java 8 + Spring Boot 2.7 + MySQL 8）。

首批实现对应设计文档：
- `design-docs/coding/01-认证与会话管理/01-01-登录认证.md`
- `design-docs/coding/01-认证与会话管理/01-01-登录认证.spec.md`

## 1. 技术栈

| 维度 | 选型 |
| --- | --- |
| JDK | Java 8 |
| 框架 | Spring Boot 2.7.18 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0（兼容 5.7+，utf8mb4） |
| 密码哈希 | spring-security-crypto（BCrypt） |
| 国密 SM4 | Hutool-crypto + BouncyCastle |
| JWT | jjwt 0.11.5（RS256） |
| 加密材料存储 | Redis（默认，key: `auth:crypto:login:{cryptoId}`）；可切换为进程内内存实现 |
| 会话吊销存储 | Redis（key: `auth:session:revoked:{sessionId}`） |

## 2. 目录结构

```
backend/
├── pom.xml
├── src/main/
│   ├── java/com/boardinggate/
│   │   ├── BoardingGateApplication.java      应用入口
│   │   ├── config/                            MVC / 异步 / MP 元字段填充
│   │   ├── web/                               统一外壳 + 全局异常
│   │   ├── auth/                              登录认证模块
│   │   │   ├── controller/                    /auth/crypto/login、/auth/refresh、/auth/logout
│   │   │   ├── service/                       AuthService / AuthSessionService / CryptoService / TokenService / SessionService / LoginLogService
│   │   │   ├── store/                         CryptoMaterialStore（Redis/内存）、SessionRevocationStore（Redis）
│   │   │   ├── util/                          SM4 / Cookie / ClientIp 工具
│   │   ├── web/
│   │   │   ├── dto/                           ApiResponse / ApiCode
│   │   │   ├── exception/                     GlobalExceptionHandler / BusinessException
│   │   │   └── security/                      JwtAuthFilter / LoginUser / @CurrentUser
│   │   │   ├── entity/                        SysSession / SysLoginLog
│   │   │   ├── mapper/                        MyBatis-Plus Mapper
│   │   │   └── dto/                           请求/响应 DTO
│   │   └── user/                              用户域
│   └── resources/
│       ├── application.yml                    主配置
│       ├── db/
│       │   ├── schema.sql                     建库建表
│       │   └── data.sql                       测试数据
│       └── keys/                              JWT RSA 密钥（可选，未提供时启动自动生成临时密钥）
```

## 3. 数据库初始化

```bash
# 1) 创建库与表
mysql -uroot -p < src/main/resources/db/schema.sql

# 2) 插入测试数据
mysql -uroot -p < src/main/resources/db/data.sql
```

测试账号（密码均为 `123456`）：

| 用户名 | 说明 | 预期行为 |
| --- | --- | --- |
| `admin` | 超级管理员，已启用 | 正常登录成功 |
| `jack` | 普通用户，已启用 | 正常登录成功 |
| `demo` | 已停用 | 返回业务码 `A0212` |
| `reset` | 需要强制改密 | 返回业务码 `A0220`，data 中仍含 accessToken |

## 4. 配置数据库与 Redis

在 `application.yml` 的 `spring.datasource` / `spring.redis` 修改连接参数，或通过环境变量覆盖：

```powershell
# MySQL
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="boarding_gate"
$env:DB_USER="root"
$env:DB_PASSWORD="your-password"

# Redis
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DB="0"
```

本地启动 Redis（任选其一）：

```bash
# Docker（推荐）
docker run -d --name boarding-gate-redis -p 6379:6379 redis:7-alpine

# Windows（安装包 / WSL2 均可，略）
```

如需临时切换为进程内内存存储（仅单机联调，不支持多实例），在 `application.yml` 中改：

```yaml
app:
  auth:
    crypto-store:
      type: memory
```

## 5. 启动

```bash
cd backend
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/boarding-gate-backend.jar
```

服务启动后监听 `http://localhost:8080/api`。

## 6. JWT 密钥（可选，推荐）

本地联调如未放置 PEM，将在启动时自动生成 2048 位 RSA 密钥对（**每次重启都会换新**，已签发的 Access Token 会失效）。

生产部署请生成固定密钥对并放入 `src/main/resources/keys/`：

```bash
# 生成 PKCS8 私钥
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
# 生成对应公钥
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
```

## 7. 接口联调

### 7.1 获取 SM4 加密参数

```bash
curl -i http://localhost:8080/api/auth/crypto/login
```

响应：

```json
{
  "code": "200",
  "message": "ok",
  "data": {
    "cryptoId": "xxx",
    "algorithm": "SM4",
    "mode": "CBC",
    "key": "Base64(16B)",
    "iv": "Base64(16B)",
    "ttlSeconds": 180
  },
  "error": null
}
```

### 7.2 登录（前端拿到 key/iv 后用 SM4-CBC/PKCS7 加密密码并转 Hex）

```bash
curl -i -X POST http://localhost:8080/api/auth/crypto/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "cryptoId": "<上一步返回的 cryptoId>",
    "password": "<SM4 密文 Hex>"
  }'
```

成功响应：

```json
{
  "code": "200",
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "expiresIn": 1800,
    "tokenType": "Bearer",
    "forceChangePassword": false
  },
  "error": null
}
```

响应头另带：

```
Set-Cookie: refreshToken=<jwt>; Path=/api/auth/refresh; Max-Age=1209600; HttpOnly; SameSite=Strict
```

### 7.3 刷新 Access Token

Refresh Token 仅存在于 HttpOnly Cookie 中，浏览器会自动随请求带上；用 curl 调试可手工带 Cookie：

```bash
curl -i -X POST http://localhost:8080/api/auth/refresh \
  -H "Cookie: refreshToken=<上一步拿到的 refreshToken>"
```

成功响应：

```json
{
  "code": "200",
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "expiresIn": 1800,
    "tokenType": "Bearer"
  },
  "error": null
}
```

注意：刷新成功后会**旋转**出新的 Refresh Token 并重置 `sys_session.refresh_jti`，旧 Refresh 即刻失效；若检测到旧 Refresh 被重放（jti 不匹配当前绑定），会主动吊销整个会话以防劫持。

### 7.4 登出

```bash
curl -i -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <accessToken>" \
  -H "Cookie: refreshToken=<refreshToken>"
```

成功响应：

```json
{ "code": "200", "message": "ok", "data": null, "error": null }
```

效果：
1. 在 Redis 写入 `auth:session:revoked:{sessionId}`，TTL 与 Refresh TTL 对齐；
2. `sys_session.status` 置 0，`expires_at` 置当前时间；
3. 响应头下发 `Set-Cookie: refreshToken=; Max-Age=0` 清除浏览器 Cookie。

该接口为**幂等操作**：即使 Access / Refresh 都无法解析，也会返回成功并清 Cookie。

### 7.5 受保护接口示例

除登录/刷新/登出外，所有业务接口均需在 `Authorization` 头中携带 `Bearer <accessToken>`：

```bash
curl -i http://localhost:8080/api/user/me \
  -H "Authorization: Bearer <accessToken>"
```

成功响应：

```json
{
  "code": "200",
  "message": "ok",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "status": 1,
    "sessionId": "..."
  },
  "error": null
}
```

常见失败场景：

| 场景 | 业务码 |
| --- | --- |
| 未带 `Authorization: Bearer ...` | `A0240` |
| Token 签名/过期/格式错误 | `A0232` |
| 对应会话已被 logout 吊销 | `A0231` |

Controller 写法（零样板）：

```java
@GetMapping("/me")
public ApiResponse<MeResp> me(@CurrentUser LoginUser user) { ... }
```

过滤器在进入 Controller 之前已完成：RS256 签名校验 → 过期校验 → `tokenType=access` 校验 →
Redis `auth:session:revoked:{sessionId}` 吊销命中检查 → 写入 `LoginUserContext`；Controller
拿到的请求必然已是登录态。

白名单路径（无需 Token）：

```
/auth/crypto/login
/auth/refresh
/auth/logout
/error
/actuator/**
/favicon.ico
```

## 8. 业务错误码

| HTTP | code | 含义 |
| --- | --- | --- |
| 200 | `"200"` | 成功 |
| 200 | `"A0210"` | 用户名或密码错误 |
| 200 | `"A0212"` | 账号已停用 |
| 200 | `"A0213"` | 加密参数无效/过期/已使用 |
| 200 | `"A0215"` | 服务端暂无法签发加密参数 |
| 200 | `"A0220"` | 需要强制修改密码（data 中仍含 accessToken） |
| 200 | `"A0230"` | Refresh Token 无效或已过期 |
| 200 | `"A0231"` | Refresh Token / 会话已被吊销 |
| 200 | `"A0232"` | Access Token 无效或已过期 |
| 200 | `"A0240"` | 未登录或凭证缺失 |
| 200 | `"A0400"` | 请求参数不合法 |
| 200 | `"B0500"` | 服务器内部错误 |

> 所有业务响应均为 HTTP 200，通过外壳中的字符串 `code` 区分；前端判断成功须用 `code === '200'`。

## 9. 后续接入点（TODO）

- [ ] 登录失败锁定（BR-02）
- [ ] 审计日志（登录/登出/刷新统一事件表）
- [ ] 权限模型（角色/资源粒度鉴权，可在过滤器之后追加授权拦截器）
