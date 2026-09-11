# qingzhou-app-oauth2

轻舟平台的 OAuth 2.0 授权服务器应用：提供授权码、密码、客户端凭证、刷新令牌与隐式五种授权能力，
并内置接入客户端、资源拥有者的管理界面。

- 应用 code：`qingzhou-app-oauth2`
- 端点前缀：`/oauth2`
- 依赖：平台 `qingzhou-jdbc` 组件提供的 `JdbcPool`（应用自身不含任何第三方依赖）

## 1. 前置条件

本应用本身不含数据库驱动，运行时需要平台先具备可用的 JDBC 连接池。以下三处需要你**自行修改**
实例配置文件 `instances/<实例>/conf/qingzhou.properties`（默认实例为 `instances/default`），
本项目的默认配置**未启用**这些项。

### 1.1 启用 qingzhou-jdbc 特性

平台默认禁用了 `qingzhou-jdbc`，需要把它从禁用列表移除：

```properties
# 修改前
-Dqingzhou.features.disabled=qingzhou-agent,qingzhou-jdbc \

# 修改后
-Dqingzhou.features.disabled=qingzhou-agent \
```

> 说明：`qingzhou-jdbc` 是本应用连接数据库所必需的组件，禁用时应用取不到 `JdbcPool`，只会记录
> 一条“数据源不可用”日志并跳过 `/oauth2/*` 端点注册（不会影响平台与其他应用启动）。

### 1.2 配置 JDBC 连接池

取消 `jdbc` 段落的注释并按需填写。`qingzhou-jdbc~h2` 中 `~` 之后的名字（这里是 `h2`）就是连接池实例名：

```properties
qingzhou-jdbc~h2.url=jdbc:h2:mem:oauth2db;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE
qingzhou-jdbc~h2.username=sa
qingzhou-jdbc~h2.password=
qingzhou-jdbc~h2.dataSourceClassName=org.h2.jdbcx.JdbcDataSource
qingzhou-jdbc~h2.maxActive=20
qingzhou-jdbc~h2.testWhileIdle=true
```

- `jdbc:h2:mem:oauth2db` 为内存库，连接池释放全部空闲连接后数据会丢失；需要持久化请改为文件库，
  例如 `jdbc:h2:file:./data/oauth2db;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE`。
- 使用其它数据库时同理：配置 `qingzhou-jdbc~<池名>.*`，把 `dataSourceClassName` 换成对应实现类
  （如 `oracle.jdbc.pool.OracleDataSource`）。

### 1.3 驱动位置

JDBC 驱动由平台从 `instances/<实例>/lib/` 加载。发行包默认不包含驱动包，请把 h2 驱动包放在
`instances/default/lib/h2-2.2.222.jar`；若使用其它数据库，把对应驱动 jar 放入该目录即可。

## 2. 应用配置项

应用级配置同样写在 `qingzhou.properties`，前缀为 `app~qingzhou-app-oauth2.`：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `app~qingzhou-app-oauth2.jdbc_name` | `h2` | 使用哪个 `JdbcPool` 实例，取值即 `qingzhou-jdbc~<name>.*` 中 `~` 之后的名字 |
| `app~qingzhou-app-oauth2.implicit_enabled` | `false` | 是否允许隐式模式（`response_type=token`） |

示例：

```properties
#app~qingzhou-app-oauth2.jdbc_name=h2
#app~qingzhou-app-oauth2.implicit_enabled=true
```

## 3. HTTP 端点

端点由应用以“免登录”方式注册在 HTTP 服务上，默认地址形如 `https://<host>:7900/oauth2/...`
（默认开启 SSL，自签证书场景可用 `curl -k`）。

| 端点 | 方法 | 用途 |
|---|---|---|
| `/oauth2/authorize` | GET / POST | 授权端点：展示登录授权页、签发授权码或隐式令牌 |
| `/oauth2/token` | POST | 令牌端点：换取、刷新令牌 |
| `/oauth2/userinfo` | GET / POST | 用户信息：凭访问令牌返回资源拥有者信息 |
| `/oauth2/introspect` | POST | 令牌校验：查询访问令牌是否有效 |
| `/oauth2/revoke` | POST | 令牌注销：撤销访问令牌及其刷新令牌 |

成功响应为 JSON；协议错误统一返回 HTTP 400（`invalid_token` 为 401），正文形如：

```json
{"error": "invalid_client", "error_description": "客户端验证失败"}
```

### 3.1 `/oauth2/authorize`

请求参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `response_type` | 是 | `code`（授权码）或 `token`（隐式，需开启 `implicit_enabled`） |
| `client_id` | 是 | 接入客户端 ID |
| `redirect_uri` | 否 | 不传时使用客户端登记的 `redirect_uri` |
| `scope` | 否 | 申请的授权范围 |
| `state` | 否 | 原样回传，用于防 CSRF |

- `GET`：校验参数后返回登录授权页。
- `POST`：在页面上提交 `userName`、`password`、`action`（`approve` / `deny`）。
  - 同意且 `response_type=code`：携带授权码回跳 `redirect_uri?code=<code>&state=<state>`。
  - 同意且 `response_type=token`：以 URL fragment 回跳
    `redirect_uri#access_token=...&token_type=bearer&expires_in=...&scope=...&state=...`。
  - 拒绝：回跳 `redirect_uri?error=access_denied&state=<state>`。

### 3.2 `/oauth2/token`

公共参数：`client_id`、`client_secret`（必填并校验）、`grant_type`。

| `grant_type` | 额外参数 | 说明 |
|---|---|---|
| `authorization_code` | `code`、`redirect_uri`（可选，需与授权时一致） | 授权码换取令牌，授权码一次性使用 |
| `password` | `userName`、`password`、`scope` | 资源拥有者密码模式 |
| `client_credentials` | `scope` | 客户端凭证模式，不关联用户 |
| `refresh_token` | `refresh_token` | 刷新令牌，刷新即轮换（旧令牌立即失效） |

成功响应：

```json
{
  "access_token": "...",
  "token_type": "bearer",
  "expires_in": 7200,
  "refresh_token": "...",
  "scope": "read"
}
```

### 3.3 `/oauth2/userinfo`

访问令牌可通过请求头 `Authorization: Bearer <access_token>` 或参数 `access_token` 传递。

```json
{
  "sub": "admin",
  "userName": "admin",
  "nickname": "管理员",
  "role": "system",
  "client_id": "test_client",
  "scope": "read"
}
```

### 3.4 `/oauth2/introspect`

参数：`token`。

```json
{"active": true, "client_id": "test_client", "userName": "admin", "scope": "read", "token_type": "bearer", "exp": 1730000000}
```

无效或已过期时返回 `{"active": false}`。

### 3.5 `/oauth2/revoke`

参数：`token`（或 `access_token`）。撤销成功返回 `{"success": true}`。

## 4. 快速开始

以默认种子数据为例（客户端 `test_client` / `test_secret`，用户 `admin` / `admin123`）。

```bash
BASE=https://localhost:7900/oauth2

# 1) 客户端凭证模式
curl -k -X POST "$BASE/token" \
  -d 'grant_type=client_credentials&client_id=test_client&client_secret=test_secret&scope=read'

# 2) 密码模式
TOKEN_JSON=$(curl -k -s -X POST "$BASE/token" \
  -d 'grant_type=password&client_id=test_client&client_secret=test_secret&userName=admin&password=admin123&scope=read')
echo "$TOKEN_JSON"

ACCESS=$(echo "$TOKEN_JSON" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
REFRESH=$(echo "$TOKEN_JSON" | sed -n 's/.*"refresh_token":"\([^"]*\)".*/\1/p')

# 3) 查询用户信息
curl -k "$BASE/userinfo" -H "Authorization: Bearer $ACCESS"

# 4) 校验令牌
curl -k -X POST "$BASE/introspect" -d "token=$ACCESS"

# 5) 刷新令牌
curl -k -X POST "$BASE/token" \
  -d "grant_type=refresh_token&client_id=test_client&client_secret=test_secret&refresh_token=$REFRESH"

# 6) 注销令牌
curl -k -X POST "$BASE/revoke" -d "token=$ACCESS"
```

授权码模式：浏览器访问 `$BASE/authorize?response_type=code&client_id=test_client&scope=read&state=abc`，
登录并同意后回调地址会带上 `code`，再用该 `code` 调用 `/oauth2/token` 换取令牌。

## 5. 管理控制台

应用在轻舟控制台“接入管理”菜单下提供两个模型：

- **接入客户端**（`client`）：维护 `client_id`、`client_secret`、`client_name`、`redirect_uri`、
  `grant_types`、`scope`。
- **资源拥有者**（`user`）：维护登录授权页的 `userName`、`password`、`nickname`、`roleName`。

首次访问数据库时会自动建表并写入种子数据：

| 表 | 说明 |
|---|---|
| `oauth_client` | 接入客户端 |
| `oauth_user` | 资源拥有者 |
| `oauth_auth_code` | 授权码（10 分钟过期、一次性使用） |
| `oauth_token` | 访问令牌（2 小时）与刷新令牌（7 天） |

种子数据：客户端 `test_client` / `test_secret`；用户 `admin` / `admin123`（role `system`）、
`test` / `test123`（role `auditor`）。**上线前请务必修改或删除这些示例数据。**

## 6. 安全说明

- 所有 `/oauth2/*` 端点对平台免登录，协议安全依赖客户端凭据与访问令牌本身。
- `redirect_uri` 仅允许 `http`/`https`，并拦截 CRLF 注入，防止授权码被重定向到本地文件或脚本。
- 当前实现中，`client_secret` 与用户口令均为明文存储/比对，`/oauth2/introspect` 与
  `/oauth2/revoke` 不校验调用方客户端身份。生产环境请结合网关、网络隔离或后续加固使用。

## 7. 排错

| 现象 | 排查方向 |
|---|---|
| 日志出现“oauth2 数据源不可用”，`/oauth2/*` 未注册 | 确认已按第 1 节启用 `qingzhou-jdbc` 并放开 `qingzhou-jdbc~h2.*` |
| 报 `ClassNotFoundException: org.h2.jdbcx.JdbcDataSource` | 驱动不在 `instances/<实例>/lib/`，放入对应驱动 jar 后重启 |
| 报 `JdbcPool[h2] 不可用` | 池实例名不匹配：`jdbc_name` 要与 `qingzhou-jdbc~<name>.*` 中 `~` 后的名字一致 |
| 授权回调报 `invalid_client` / `invalid_request` | 检查 `client_id` 是否存在、`redirect_uri` 是否与登记值一致 |
