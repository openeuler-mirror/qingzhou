# qingzhou-app-oauth2

轻舟平台的 OAuth 2.0 授权服务器应用：提供授权码、密码、客户端凭证、刷新令牌与隐式五种授权能力，
并内置接入客户端、资源拥有者的管理界面。

- 应用 code：`qingzhou-app-oauth2`
- 端点前缀：`/oauth2-server`
- 依赖：平台 `qingzhou-jdbc` 组件提供的 `JdbcPool`、`qingzhou-crypto` 组件提供的凭据摘要能力
  （应用自身不含任何第三方依赖）

## 1. 前置条件

本应用本身不含数据库驱动，运行时需要平台先具备可用的 JDBC 连接池。以下三处需要你**自行修改**
实例配置文件 `instances/<实例>/conf/qingzhou.properties`（默认实例为 `instances/default`），
本项目的默认配置**未启用**这些项。

### 1.1 启用 qingzhou-jdbc 特性

> 说明：`qingzhou-jdbc` 是本应用连接数据库所必需的组件，禁用时应用取不到 `JdbcPool`，只会记录
> 一条“数据源不可用”日志并跳过 `/oauth2-server/*` 端点注册（不会影响平台与其他应用启动）。

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
| `app~qingzhou-app-oauth2.implicit_enabled` | `false` | 是否允许隐式模式（`response_type=token`），客户端 `grant_types` 还需包含 `implicit` |
| `app~qingzhou-app-oauth2.seed_demo` | `false` | 是否写入演示客户端与用户；演示口令为公开的固定值，仅限试用环境 |

示例：

```properties
#app~qingzhou-app-oauth2.jdbc_name=h2
#app~qingzhou-app-oauth2.implicit_enabled=true
#app~qingzhou-app-oauth2.seed_demo=true
```

## 3. HTTP 端点

端点由应用以“免登录”方式注册在 HTTP 服务上，默认地址形如 `https://<host>:7900/oauth2-server/...`
（默认开启 SSL，自签证书场景可用 `curl -k`）。

> 前缀取 `/oauth2-server` 而非 `/oauth2`：平台的单点登录客户端模块 `qingzhou-oauth2` 独占 `/oauth2/`
> （`/oauth2/authorize` 与 `/oauth2/callback` 是该客户端自身的跳转入口与回调），路径重叠会让本应用的
> 端点注册冲突、启动失败。若把本应用作为平台单点登录的授权服务器，把 `qingzhou-oauth2.authorize_endpoint`、
> `token_endpoint`、`userinfo_endpoint` 指向 `.../oauth2-server/...` 即可；客户端的回调地址
> （`redirect_uri` + `/oauth2/callback`）属于 `qingzhou-oauth2` 模块，保持原样。

| 端点 | 方法 | 调用方身份 | 用途 |
|---|---|---|---|
| `/oauth2-server/authorize` | GET（展示授权页）/ POST（提交授权） | 资源拥有者登录 | 签发授权码或隐式令牌 |
| `/oauth2-server/token` | POST | 客户端凭据（`client_id` + `client_secret`） | 换取、刷新令牌 |
| `/oauth2-server/userinfo` | GET / POST | 访问令牌 | 返回资源拥有者信息 |
| `/oauth2-server/introspect` | POST | 客户端凭据 | 查询访问令牌是否有效 |
| `/oauth2-server/revoke` | POST | 客户端凭据 | 撤销访问令牌及其刷新令牌 |

成功响应为 JSON；协议错误统一返回 HTTP 400（`invalid_token` 为 401），正文形如：

```json
{"error": "invalid_client", "error_description": "客户端验证失败"}
```

### 3.1 `/oauth2-server/authorize`

请求参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `response_type` | 是 | `code`（授权码）或 `token`（隐式，需开启 `implicit_enabled` 且客户端登记 `implicit`） |
| `client_id` | 是 | 接入客户端 ID |
| `redirect_uri` | 否 | 必须与客户端登记的 `redirect_uri` 完全一致；不传时使用登记值 |
| `scope` | 否 | 申请的授权范围，不得超出客户端登记的 `scope` |
| `state` | 否 | 原样回传，用于防 CSRF |

- `GET`：校验参数后返回登录授权页。
- `POST`：在页面上提交 `userName`、`password`、`action`（`approve` / `deny`）。
  - 同意且 `response_type=code`：携带授权码回跳 `redirect_uri?code=<code>&state=<state>`。
  - 同意且 `response_type=token`：以 URL fragment 回跳
    `redirect_uri#access_token=...&token_type=bearer&expires_in=...&scope=...&state=...`。
  - 拒绝：回跳 `redirect_uri?error=access_denied&state=<state>`。

### 3.2 `/oauth2-server/token`

公共参数：`client_id`、`client_secret`（必填并校验）、`grant_type`（必须在该客户端登记的
`grant_types` 内）。

| `grant_type` | 额外参数 | 说明 |
|---|---|---|
| `authorization_code` | `code`、`redirect_uri`（可选，需与授权时一致） | 授权码换取令牌，授权码一次性使用 |
| `password` | `userName`、`password`、`scope` | 资源拥有者密码模式 |
| `client_credentials` | `scope` | 客户端凭证模式，不关联用户 |
| `refresh_token` | `refresh_token` | 刷新令牌，刷新即轮换（旧令牌立即失效） |

`scope` 不得超出客户端登记的 `scope`，超出时返回 `invalid_scope`。

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

### 3.3 `/oauth2-server/userinfo`

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

### 3.4 `/oauth2-server/introspect`

参数：`token`，另需 `client_id`、`client_secret`；只能校验该客户端自己签发的令牌。

```json
{"active": true, "client_id": "test_client", "userName": "admin", "scope": "read", "token_type": "bearer", "exp": 1730000000}
```

无效、已过期或不属于该客户端时返回 `{"active": false}`。

### 3.5 `/oauth2-server/revoke`

参数：`token`（或 `access_token`），另需 `client_id`、`client_secret`；只能注销该客户端自己签发的令牌。
撤销成功返回 `{"success": true}`。

## 4. 快速开始

先开启演示数据（`app~qingzhou-app-oauth2.seed_demo=true` 并重启），即得到客户端
`test_client` / `test_secret`，用户 `admin` / `admin123`。

```bash
BASE=https://localhost:7900/oauth2-server
CRED='client_id=test_client&client_secret=test_secret'

# 1) 客户端凭证模式
curl -k -X POST "$BASE/token" \
  -d "grant_type=client_credentials&$CRED&scope=read"

# 2) 密码模式
TOKEN_JSON=$(curl -k -s -X POST "$BASE/token" \
  -d "grant_type=password&$CRED&userName=admin&password=admin123&scope=read")
echo "$TOKEN_JSON"

ACCESS=$(echo "$TOKEN_JSON" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
REFRESH=$(echo "$TOKEN_JSON" | sed -n 's/.*"refresh_token":"\([^"]*\)".*/\1/p')

# 3) 查询用户信息
curl -k "$BASE/userinfo" -H "Authorization: Bearer $ACCESS"

# 4) 校验令牌
curl -k -X POST "$BASE/introspect" -d "token=$ACCESS&$CRED"

# 5) 刷新令牌
curl -k -X POST "$BASE/token" \
  -d "grant_type=refresh_token&$CRED&refresh_token=$REFRESH"

# 6) 注销令牌
curl -k -X POST "$BASE/revoke" -d "token=$ACCESS&$CRED"
```

授权码模式：浏览器访问 `$BASE/authorize?response_type=code&client_id=test_client&scope=read&state=abc`
（`redirect_uri` 省略时使用登记值 `https://localhost:7900/oauth2/callback`），
登录并同意后回调地址会带上 `code`，再用该 `code` 调用 `/oauth2-server/token` 换取令牌。

## 5. 管理控制台

应用在轻舟控制台“接入管理”菜单下提供两个模型：

- **接入客户端**（`client`）：维护 `client_id`、`client_secret`、`client_name`、`redirect_uri`、
  `grant_types`、`scope`。
- **资源拥有者**（`user`）：维护登录授权页的 `userName`、`password`、`nickname`、`roleName`。

`client_secret` 与 `password` 只落库加盐迭代摘要（`SHA-256$salt$iterations$digest`），
新增/修改时填写明文，编辑时留空即保留原值，界面也不会回显摘要。升级前写入的明文记录无法再用于
认证，需在控制台重新保存。

首次访问数据库时会自动建表：

| 表 | 说明 |
|---|---|
| `oauth_client` | 接入客户端 |
| `oauth_user` | 资源拥有者 |
| `oauth_auth_code` | 授权码（10 分钟过期、一次性使用） |
| `oauth_token` | 访问令牌（2 小时）与刷新令牌（7 天） |

种子数据仅在 `seed_demo=true` 时写入：客户端 `test_client` / `test_secret`；用户 `admin` / `admin123`
（role `system`）、`test` / `test123`（role `auditor`）。这些口令是公开的固定值，
**只能在试用环境开启**；生产环境请在控制台自行创建客户端与用户。

## 6. 安全说明

- 所有 `/oauth2-server/*` 端点对平台免登录，协议安全依赖客户端凭据与访问令牌本身。
- `redirect_uri` 必须与客户端登记值完全一致，且仅允许 `http`/`https`、拦截 CRLF 注入，
  避免授权码或令牌被投递到攻击者地址。
- `client_secret` 与用户口令以平台 `qingzhou-crypto` 的加盐迭代摘要
  （`SHA-256$salt$iterations$digest`）落库，校验时比对摘要而非明文，明文不入库、不写日志。
- `/oauth2-server/token`、`/oauth2-server/introspect`、`/oauth2-server/revoke` 仅接受 POST（避免凭据进入 URL 与访问日志），
  且 `introspect`/`revoke` 必须出示客户端凭据并只能操作自己名下的令牌。
- 授予方式与授权范围都受客户端登记值约束：`grant_type` 必须在 `grant_types` 内，
  `scope` 不得超出客户端 `scope`。
- 授权码一次性使用、刷新令牌刷新即轮换，二者均以带条件的原子更新核销，并发重复提交只有一次生效。
- 授权页登录与密码模式按来源限流，连续 5 次失败锁定 5 分钟。
- 令牌与用户信息响应带 `Cache-Control: no-store`，授权页额外带 `X-Frame-Options: DENY`。
- 生产环境仍建议叠加网关、网络隔离与 HTTPS 证书校验。

## 7. 排错

| 现象 | 排查方向 |
|---|---|
| 日志出现“oauth2 数据源不可用”，`/oauth2-server/*` 未注册 | 确认已按第 1 节启用 `qingzhou-jdbc` 并放开 `qingzhou-jdbc~h2.*` |
| 报 `ClassNotFoundException: org.h2.jdbcx.JdbcDataSource` | 驱动不在 `instances/<实例>/lib/`，放入对应驱动 jar 后重启 |
| 报 `JdbcPool[h2] 不可用` | 池实例名不匹配：`jdbc_name` 要与 `qingzhou-jdbc~<name>.*` 中 `~` 后的名字一致 |
| 授权回调报 `invalid_client` / `invalid_request` | 检查 `client_id` 是否存在、`redirect_uri` 是否与登记值完全一致 |
| 报 `invalid_scope` / `unauthorized_client` | 申请的 `scope` 或 `grant_type` 超出了客户端登记范围，在控制台补齐 |
| 客户端或用户始终验证失败 | 凭据为升级前的明文记录，在控制台重新保存一次 |
| 启动报 `HANDLE_PATH(...) of [@App] conflicts: /oauth2/` | 端点前缀与平台单点登录客户端模块重叠：本应用用 `/oauth2-server`，`qingzhou-oauth2` 用 `/oauth2/`，不要把 `qingzhou-oauth2.*_endpoint` 指回本应用以外的同名路径 |
