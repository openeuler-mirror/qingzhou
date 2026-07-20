# Qingzhou App Redis

Redis 日常运维管理控制台，支持单机（standalone）、哨兵（sentinel）、集群（cluster）三种模式。

## 配置 Redis 实例

Redis 实例通过配置文件管理，不支持在界面中新增或删除。在轻舟产品包的 `instances/default/conf/qingzhou.properties` 文件中配置 Redis 实例，修改后重启轻舟实例生效。

配置项格式为：

```
app~qingzhou-app-redis.{序号}.{属性名}=值
```

其中 `{序号}` 为实例编号（如 `1`、`2`、`3`），每个序号代表一个独立的 Redis 实例配置。

### 配置示例

#### 单机模式（Standalone）

```properties
app~qingzhou-app-redis.1.active=true
app~qingzhou-app-redis.1.name=本地开发
app~qingzhou-app-redis.1.mode=standalone
app~qingzhou-app-redis.1.host=127.0.0.1
app~qingzhou-app-redis.1.port=6379
app~qingzhou-app-redis.1.database=0
app~qingzhou-app-redis.1.password=
app~qingzhou-app-redis.1.envType=development
```

#### 哨兵模式（Sentinel）

```properties
app~qingzhou-app-redis.2.active=false
app~qingzhou-app-redis.2.name=哨兵集群
app~qingzhou-app-redis.2.mode=sentinel
app~qingzhou-app-redis.2.sentinelNodes=192.168.1.101:26379\n192.168.1.102:26379\n192.168.1.103:26379
app~qingzhou-app-redis.2.sentinelMaster=mymaster
app~qingzhou-app-redis.2.password=
app~qingzhou-app-redis.2.database=0
app~qingzhou-app-redis.2.envType=production
```

> `sentinelNodes` 多个节点用 `\n` 分隔。

#### 集群模式（Cluster）

```properties
app~qingzhou-app-redis.3.active=false
app~qingzhou-app-redis.3.name=生产集群
app~qingzhou-app-redis.3.mode=cluster
app~qingzhou-app-redis.3.clusterNodes=192.168.1.201:6379\n192.168.1.202:6379\n192.168.1.203:6379
app~qingzhou-app-redis.3.password=
app~qingzhou-app-redis.3.envType=production
```

> `clusterNodes` 多个节点用 `\n` 分隔。集群模式只支持 database 0。

### 配置项说明

| 属性名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `active` | 否 | `false` | 设为 `true` 时，轻舟启动后自动连接该实例 |
| `name` | 否 | 序号 | 实例显示名称，用于界面展示和切换 |
| `mode` | 是 | `standalone` | 连接模式：`standalone`、`sentinel`、`cluster` |
| `host` | standalone 必填 | `127.0.0.1` | Redis 服务器地址 |
| `port` | standalone 必填 | `6379` | Redis 服务器端口 |
| `database` | 否 | `0` | 选择的数据库编号（集群模式下固定为 0） |
| `password` | 否 | 空 | Redis 认证密码，留空表示无密码 |
| `envType` | 否 | `development` | 环境类型：`development`、`test`、`production` |
| `sentinelNodes` | sentinel 必填 | - | 哨兵节点地址列表，用 `\n` 分隔 |
| `sentinelMaster` | sentinel 必填 | `mymaster` | 哨兵监控的 master 名称 |
| `clusterNodes` | cluster 必填 | - | 集群节点地址列表，用 `\n` 分隔 |

### 密码加密

密码支持加密存储，以 `ENC:` 前缀标识。加密方式为 Base64 编码。

例如，密码 `mypassword` 的 Base64 编码为 `bXlwYXNzd29yZA==`，配置如下：

```properties
app~qingzhou-app-redis.1.password=ENC:bXlwYXNzd29yZA==
```

不加密时直接填写明文：

```properties
app~qingzhou-app-redis.1.password=mypassword
```

### 环境类型

`envType` 用于控制写操作的安全级别：

| 环境类型 | 说明 |
|----------|------|
| `development` | 开发环境，写操作无需确认 |
| `test` | 测试环境，写操作需二次确认 |
| `production` | 生产环境，写操作需二次确认，危险操作禁止执行 |

### 多实例配置

可以同时配置多个 Redis 实例，通过 `{序号}` 区分。启动时 `active=true` 的实例会自动连接。
启动后也可在「实例管理」页面通过「切换」按钮切换当前使用的实例。

```properties
# 实例 1 - 开发环境，启动时自动连接
app~qingzhou-app-redis.1.active=true
app~qingzhou-app-redis.1.name=开发环境
app~qingzhou-app-redis.1.mode=standalone
app~qingzhou-app-redis.1.host=127.0.0.1
app~qingzhou-app-redis.1.port=6379

# 实例 2 - 生产环境，不自动连接
app~qingzhou-app-redis.2.active=false
app~qingzhou-app-redis.2.name=生产环境
app~qingzhou-app-redis.2.mode=cluster
app~qingzhou-app-redis.2.clusterNodes=10.0.0.1:6379\n10.0.0.2:6379
app~qingzhou-app-redis.2.envType=production
```

> 同一时间只能使用一个实例。在「实例管理」页面点击「切换」按钮可切换当前连接的实例。

## 常见问题

**Q: 启动后页面提示"请先切换到一个 Redis 实例"？**

A: 说明没有配置任何 `active=true` 的实例，或配置的实例连接失败。请检查 `qingzhou.properties` 中是否有 `active=true` 的实例，并确保 Redis 服务可达。也可在「实例管理」页面手动切换到一个已配置的实例。

**Q: 连接提示密码错误？**

A: 检查 `password` 配置项。如果使用了 `ENC:` 前缀，确保后面跟的是密码的 Base64 编码。

**Q: 集群模式连接失败？**

A: 集群模式下 `database` 必须为 0。确保 `clusterNodes` 中至少有一个节点是可用的，且节点间可以互相通信。
