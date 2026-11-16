# qingzhou-app-oauth

轻舟平台的 OAuth 2.0 / OpenID Connect 1.0 授权服务器应用。支持授权码（含 PKCE）、密码、客户端凭证、
刷新令牌、隐式模式，OIDC ID Token、OIDC Discovery、JWKS、用户信息、令牌内省、令牌撤销和 RFC 8414 元数据；
同时提供接入客户端与资源拥有者管理界面。

- 应用 code：`qingzhou-app-oauth`
- 端点前缀：`/oauth-server`
- 默认存储：**内存**（无需数据库，进程重启后数据清空）
- 可选存储：JDBC（默认 H2；其他数据库需在应用 `pom.xml` 中添加对应驱动后重新构建）
- 依赖：平台 `qingzhou-http-server`、`qingzhou-json`、`qingzhou-crypto`；JDBC 模式额外使用平台 `qingzhou-jdbc`

## 1. 存储模式

### 1.1 内存模式（默认）

```properties
# 可省略，默认就是 memory
app~qingzhou-app-oauth.storage=memory
```

- 不需要数据库、不需要 `qingzhou-jdbc`，应用启动即可用。
- 客户端、用户、授权码、令牌都保存在进程内；重启后清空。
- 可用 `seed_demo=true` 写入演示客户端与用户；演示客户端 scope 含 `openid profile email read write`，
  演示口令仅用于试用环境。

### 1.2 JDBC 模式

应用整体默认仍是内存模式；切到 JDBC 后默认使用 H2（`jdbc_name=h2`），H2 驱动已包含在应用 fatjar 中。

```properties
app~qingzhou-app-oauth.storage=jdbc
app~qingzhou-app-oauth.jdbc_name=h2

# 平台 JDBC 连接池：~ 后的名字对应 jdbc_name
qingzhou-jdbc~h2.url=jdbc:h2:file:./data/oauth;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
qingzhou-jdbc~h2.username=sa
qingzhou-jdbc~h2.password=
```

应用使用标准 JDBC `DataSource` 与平台 `JdbcPool` 对接，不绑定任何厂商实现。建表脚本位于
`src/main/resources/sql/schema.sql`，首次启动按表存在性自动初始化，使用字符串主键和标准 SQL，
不依赖 `IDENTITY`/`AUTO_INCREMENT`，以兼容多种数据库。

**应用默认只声明 H2 驱动**。如需 PostgreSQL/MySQL/MariaDB/SQLite/SQL Server 等数据库，先在
[apps/qingzhou-app-oauth/pom.xml](pom.xml) 添加对应驱动依赖，再重新执行构建；fatjar 会把新依赖一起打包。
常用驱动坐标：

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.7.4</version>
</dependency>
<!-- MySQL：com.mysql:mysql-connector-j；MariaDB：org.mariadb.jdbc:mariadb-java-client；
     SQLite：org.xerial:sqlite-jdbc；SQL Server：com.microsoft.sqlserver:mssql-jdbc -->
```

常见数据库连接示例（记得先在上面的 pom 中添加驱动）：

```properties
# PostgreSQL
qingzhou-jdbc~pg.url=jdbc:postgresql://127.0.0.1:5432/oauth
qingzhou-jdbc~pg.username=oauth
qingzhou-jdbc~pg.password=******

# MySQL
qingzhou-jdbc~mysql.url=jdbc:mysql://127.0.0.1:3306/oauth?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
qingzhou-jdbc~mysql.username=oauth
qingzhou-jdbc~mysql.password=******

# MariaDB
qingzhou-jdbc~maria.url=jdbc:mariadb://127.0.0.1:3306/oauth
qingzhou-jdbc~maria.username=oauth
qingzhou-jdbc~maria.password=******

# SQLite（文件型，注意并发写限制）
qingzhou-jdbc~sqlite.url=jdbc:sqlite:./data/oauth.db

# SQL Server
qingzhou-jdbc~mssql.url=jdbc:sqlserver://127.0.0.1:1433;databaseName=oauth
qingzhou-jdbc~mssql.username=sa
qingzhou-jdbc~mssql.password=******
```

- 驱动类通常按 URL 自动推断；自定义驱动可配置 `app~qingzhou-app-oauth.jdbc_driver`。
- 没有加入 pom 的数据库驱动不会出现在 fatjar 中，运行时加载驱动会失败。
- 如果应用侧需要覆盖 URL/账号（例如平台连接池未配置），可使用 `jdbc_url`、`jdbc_username`、
  `jdbc_password`；平台连接池配置优先。
- 从旧版 H2 表结构升级时，旧表的 `IDENTITY` 主键与新脚本不兼容；OIDC 还新增了
  `oauth_auth_code.nonce`、`oauth_auth_code.auth_time`、`oauth_user.email` 三列，请新建库或手工迁移，
  本应用不做自动迁移。
- `jdbc_name` 默认 `h2`，对应 `qingzhou-jdbc~h2.*`。

## 2. 应用配置项

前缀：`app~qingzhou-app-oauth.`

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `storage` | `memory` | `memory` 或 `jdbc` |
| `jdbc_name` | `h2` | JDBC 模式下平台连接池实例名 |
| `jdbc_url` / `jdbc_username` / `jdbc_password` | 空 | 应用侧 JDBC 兜底配置 |
| `jdbc_driver` | 空 | 自定义 JDBC 驱动类；留空按 URL 推断 |
| `seed_demo` | `false` | 写入 `test_client/test_secret`、`admin/admin123`、`test/test123` |
| `implicit_enabled` | `false` | 是否允许 `response_type=token/id_token/id_token token` |
| `pkce_required` | `false` | 是否全局强制 PKCE（客户端也可单独开启） |
| `revoke_allow_anonymous` | `true` | 允许仅携带 token 的匿名注销；设为 `false` 则严格校验客户端凭据 |
| `issuer` | 空 | 授权服务器 issuer；为空时按请求 Host 推导；OIDC 需保持稳定 |
| `scopes_supported` | `read,write,openid,profile,email` | 发现端点公布的 scope |
| `oidc_private_key` | 空 | 可选，PKCS#8 RSA 私钥（PEM）；为空时启动生成临时密钥 |
| `oidc_key_id` | 空 | 可选，JWKS/ID Token 的 kid；为空时按公钥摘要生成 |
| `default_scope` | `read` | 未申请 scope 时的默认值 |
| `access_token_ttl` | `7200` | 访问令牌默认有效期（秒） |
| `refresh_token_ttl` | `604800` | 刷新令牌默认有效期（秒） |
| `auth_code_ttl` | `600` | 授权码有效期（秒） |

客户端可单独设置 `access_token_ttl`、`refresh_token_ttl`，覆盖全局默认值。

## 3. HTTP 端点

端点以“免登录”方式注册；协议错误返回 JSON，授权端点错误在 redirect_uri 合法时按 OAuth 规范回跳。

| 端点 | 方法 | 用途 |
| --- | --- | --- |
| `/oauth-server/authorize` | GET/POST | 登录授权页，签发授权码或隐式令牌；支持 PKCE |
| `/oauth-server/token` | POST | 授权码、密码、客户端凭证、刷新令牌 |
| `/oauth-server/userinfo` | GET/POST | Bearer 访问令牌换取用户信息 |
| `/oauth-server/introspect` | POST | 令牌内省（RFC 7662） |
| `/oauth-server/revoke` | POST | 令牌撤销（RFC 7009） |
| `/oauth-server/.well-known/oauth-authorization-server` | GET | OAuth 授权服务器元数据（RFC 8414） |
| `/oauth-server/.well-known/openid-configuration` | GET | OIDC Discovery 元数据 |
| `/oauth-server/jwks.json` | GET | ID Token 验签公钥（JWKS） |

客户端认证支持：

- `client_secret_basic`（`Authorization: Basic ...`，凭据按表单规则编码）
- `client_secret_post`（表单 `client_id` + `client_secret`）
- `none`（公开客户端；授权码流程必须配合 PKCE，建议同时设置 `require_pkce=true`）

授权码流程支持 PKCE `plain` 与 `S256`；授权码一次性使用、默认 10 分钟过期。刷新令牌默认轮换，
旧刷新令牌立即失效；`client_credentials` 与隐式模式不签发刷新令牌。

### OIDC 支持

客户端 `scope` 包含 `openid` 时启用 OIDC；`profile` 返回昵称/用户名/角色，`email` 返回邮箱。
已有客户端需把 `openid`（以及 `profile`/`email`）加入客户端 scope 后重新保存；服务端
`scopes_supported` 也建议包含这些 scope。
授权码流程在 `/token` 响应中增加 `id_token`；隐式流程支持：

- `response_type=id_token`：只返回 ID Token；
- `response_type=id_token token`：返回 Access Token 与 ID Token；
- 隐式 ID Token 必须携带 `nonce`。

ID Token 使用 RS256 签名，Claims 包括 `iss/sub/aud/exp/iat/auth_time/nonce/at_hash`，按 scope 附带
`name/preferred_username/email/role/roles`。客户端通过 `jwks_uri` 获取公钥验签。

默认每次启动生成临时 RSA 密钥，重启后旧 ID Token 将无法用新 JWKS 验签；生产环境建议通过
`oidc_private_key` 配置固定的 PKCS#8 RSA 私钥（PEM），并用稳定的 `issuer`。properties 中可将私钥
写成单行并把换行写成 `\n`；`oidc_key_id` 可固定 `kid`，便于密钥轮换时区分。

## 4. 快速开始

```properties
app~qingzhou-app-oauth.storage=memory
app~qingzhou-app-oauth.seed_demo=true
```

```bash
BASE=http://127.0.0.1:7900/oauth-server
AUTH='-u test_client:test_secret'

# 客户端凭证（test_client 默认使用 client_secret_basic）
curl -s $AUTH -X POST "$BASE/token" -d 'grant_type=client_credentials&scope=read'

# 密码模式
TOKEN=$(curl -s $AUTH -X POST "$BASE/token" \
  -d 'grant_type=password&userName=admin&password=admin123&scope=read')

# 用户信息
curl -s "$BASE/userinfo" -H "Authorization: Bearer <access_token>"

# 内省 / 撤销
curl -s $AUTH -X POST "$BASE/introspect" -d "token=<access_token>"
curl -s $AUTH -X POST "$BASE/revoke" -d "token=<access_token>"

# 发现
curl -s "$BASE/.well-known/oauth-authorization-server"
curl -s "$BASE/.well-known/openid-configuration"
curl -s "$BASE/jwks.json"
```

OIDC 授权码 + PKCE 流程（scope 必须含 `openid`，建议带 `nonce`）：

1. 浏览器访问
   `/oauth-server/authorize?response_type=code&client_id=test_client&redirect_uri=...&scope=read&state=...&code_challenge=...&code_challenge_method=S256`
2. 登录并同意后，回调地址带 `code`；
3. `POST /oauth-server/token` 使用 `grant_type=authorization_code`、`code`、`redirect_uri`、`code_verifier` 换取令牌；
   当 scope 含 `openid` 时响应中同时返回 `id_token`。

OIDC 隐式示例（需 `implicit_enabled=true` 且客户端登记 `implicit`）：

```text
/oauth-server/authorize?response_type=id_token&client_id=test_client&redirect_uri=...&scope=openid%20profile%20email&state=...&nonce=...
```

## 5. 管理控制台

“接入管理”菜单下提供两个模型：

- **接入客户端**（`client`）：`client_id`、`client_secret`、`client_name`、多个 `redirect_uri`、
  `grant_types`、`scope`、`token_endpoint_auth_method`、`require_pkce`、令牌 TTL、`enabled`。
- **资源拥有者**（`user`）：`userName`、`password`、`nickname`、`email`、`roleName`、`enabled`。

**接入客户端与资源拥有者是两套账号，不能混用：**

- 授权页登录、密码模式使用“资源拥有者”的 `userName` / `password`；
- `client_id` / `client_secret` 只用于 OAuth 客户端认证，不是登录账号；
- 默认 `seed_demo=false`，新建应用后必须在“资源拥有者”中先创建用户，否则授权页会提示尚未创建资源拥有者账号；
- 新增用户时密码必填；编辑用户时密码留空表示保留原密码，填写新值即重置密码；
- 演示模式创建的用户为 `admin/admin123` 与 `test/test123`。

`client_secret` 与 `password` 新写入只保存加盐迭代摘要；新增/修改时填写明文，编辑时留空保留原值，界面不回显摘要。
为兼容历史数据库/外部导入数据，校验时还接受平台全局密钥 AES 密文与早期明文值；密文/明文不会进入 PBKDF2，
只有摘要格式才做摘要校验，并对口令长度与盐长度设上限，避免异常历史值造成长时间计算。建议在升级后重置一次历史客户端密钥，使存储恢复为摘要。
授予类型为多选下拉（无默认值，需操作人明确选择）；客户端认证方式为单选下拉（默认 `client_secret_basic`）；`启用`、`强制 PKCE` 为开关；字段说明中给出了各标准值的中文含义。

每个接入客户端行提供 **“接入示例”** 操作，打开该客户端独立的接入说明页：

```text
/oauth-server/guide?client_id=<client_id>
```

页面包含端点、授权模式、scope、回调地址和可复制的 curl 示例，适合直接发给客户端开发方。页面为只读 HTML，
**不包含 client_secret**，命令中使用 `<YOUR_CLIENT_SECRET>` 占位；如客户端是 `none` 公开客户端则显示“无需密钥”。
示例页是公开只读页面，可按 client_id 分享；如不希望暴露客户端配置，请不要对外发布该链接。
管理端点击“接入示例”时优先在新标签打开；浏览器拦截弹窗时回退到当前标签打开，操作提示中也会给出页面路径。

## 6. 构建

```bash
mvn -pl apps/qingzhou-app-oauth -am clean package -DskipTests
```

产物 `apps/qingzhou-app-oauth/target/qingzhou-app-oauth.jar` 是包含**当前 pom 声明的 JDBC 驱动**
的 fatjar（默认只有 H2）。放入 `<实例>/apps/` 后重启轻舟即可；其他数据库驱动按上面说明加入
`pom.xml` 后重新构建，不依赖实例 `lib/` 中的驱动。

## 7. 安全与兼容性

- 授权码、刷新令牌一次性/轮换；客户端密钥与用户口令只存摘要。
- `redirect_uri` 精确匹配，支持多个登记值；授权码/隐式模式必填，仅客户端凭证/密码模式可留空；拒绝 CRLF 与 fragment。
- 接入客户端新增/更新的校验错误会以 400 + 可读消息返回表单，不再表现为无说明的“操作失败”。
- 接入示例页为只读 HTML，不包含 client_secret；公开客户端无需密钥。
- 登录失败按来源+客户端限流；授权页禁止被 iframe 嵌入，响应禁止缓存。
- 授权页表单是同源 POST；反向代理必须保留浏览器 `Host`，否则平台跨站写保护会返回 403。
- 在 sandboxed iframe/WebView 等不透明来源中，浏览器会发送字面量 `Origin: null`；本应用仅对授权端点
  `/oauth-server/authorize` 显式允许该来源，其他端点仍返回 403。该例外不适用于携带 Cookie 会话的接口。
- 授权页登录使用“资源拥有者”账号；`client_id`/`client_secret` 不是用户账号。
- 注销端点默认允许 token-only 调用（不携带客户端凭据）；严格 RFC 7009 部署可设
  `app~qingzhou-app-oauth.revoke_allow_anonymous=false`，此时客户端必须使用 Basic 或
  `client_id`+`client_secret`。注销只销毁令牌，不签发令牌；token 建议放 POST body。
- 访问令牌不透明，内省返回 `iss/aud/sub/exp/iat/nbf` 等标准字段。
- ID Token 为 RS256 JWT，通过公开 JWKS 验签；授权码流程的 `nonce` 会原样写入 ID Token。
- SQL 使用标准类型与字符串主键；分页按数据库方言适配 `LIMIT/OFFSET` 或 `OFFSET/FETCH`。
- OIDC 当前支持授权码、隐式 ID Token 与 Discovery/JWKS；暂不支持动态客户端注册、OIDC Session Management、
  Front-Channel/Back-Channel Logout、ID Token 加密与设备码流程。
