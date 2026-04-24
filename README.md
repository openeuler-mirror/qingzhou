# Qingzhou（轻舟）融合管理开发平台

## 项目简介

轻舟可用于集中式管理您的各类业务系统。

## 开始使用

### 欢迎

### 快速入门

1. **源码编译**

确保您已经安装了 JDK 1.8+ 和 Maven 3.8+。

在源码根目录执行 `mvn clean install -DskipTests`，执行完成后，可在 `qingzhou/target/qingzhou` 得到轻舟的二进制产品安装包。

- bin: 可执行程序目录。
  - start.sh: Linux/Mac 平台启动脚本，可以“空格+实例名”指定要启动的实例（即 instances 目录下的子文件名），默认为 default。
  - start.bat: Windows 平台启动脚本，可以“空格+实例名”指定要启动的实例（即 instances 目录下的子文件名）），默认为 default。
  - gen-cipher-key.sh: 生成随机的对称加密密钥，用于 qingzhou.crypto.Crypto.getCipher(String key) 服务。
  - gen-pair-key.sh: 生成随机的非对称公钥和私钥对，用于 qingzhou.crypto.Crypto.getPairCipher(String publicKey, String privateKey) 服务。
- instances: 实例数据目录，可根据业务需求复制多份实例目录，如：开发、测试、OA、门户等。
  - default: 默认启动的实例目录。
- lib: 存放轻舟源码编译后的二进制 *.jar 文件。
  - version*.zip: 轻舟二进制 *.jar 文件的分发 zip 包，启动时会自动解压开，解压后此文件不再需要，可删除或继续留存。
  - version* 目录: 在启动时根据 version*.zip 自动解压生成的目录，此目录下的 *.jar 文件会加载到内存。
  - 重点说明：启动时，如果 version* 目录已存在，则会与 version*.zip 比对，不一致，则删除重新生成，若要禁止此行为，可将 version*.zip 移出此目录，或修改其名字不要以 version 开头。
  -
2. **启动**

在 `${轻舟安装包}/bin` 下，执行 start 脚本，如：sh start.sh，等待启动完成。

3. **使用**

轻舟提供了可视化的 Web 管理控制台，如已部署，可打开浏览器访问：

[http://localhost:7900/console](http://localhost:7900/console)

注：轻舟的 Web 管理控制台支持独立部署，因此可能并未部署在当前轻舟实例上，请根据实际部署情况访问。

4. **应用开发**

📌 参考示例：`apps/qingzhou-demo-app`。

1. **创建模块**：在 `qingzhou\apps` 目录下创建 Maven 子模块。

2. **应用入口类开发**：
  - 创建类，实现 `QingzhouApp` 接口。
  - 添加 `@App`、`@Menu` 注解配置元数据。
  - 重写 `start()` 方法，定制启动逻辑。

   | 注解 | 作用说明 |
      |:---|:---|
   | `@App` | 标注应用入口 |
   | `@Menu` | 标注应用菜单 |

3. **业务模型类开发**：
  - 创建类，继承 `ModelBase`。
  - 根据业务需求，实现相应的能力接口。

   **模型能力接口：**

   | 接口 | 功能说明 |
      |:---|:---|
   | `List` | 列表查询 |
   | `Show` | 详情展示 |
   | `Add` | 新增数据 |
   | `Update` | 编辑数据 |
   | `Delete` | 删除操作 |
   | `Monitor` | 实时监控 |

   **模型类常用注解：**

   | 注解 | 作用说明 |
      |:---|:---|
   | `@Model` | 标注应用模块 |
   | `@ModelField` | 标注模块字段 |
   |  `@ModelAction` | 标注模块自定义操作，访问格式：`/invoke/-/demo/model/action` |

5. **应用部署**

-  执行 `mvn clean install` 编译。
-  将生成的 jar 包放入`qingzhou\target\qingzhou\instances\default\apps` 目录下。
-  重启轻舟实例即可生效。

### 云端

## 功能

## 更多信息

### 参数配置

参数配置文件在 `${轻舟安装包}/instances/default/conf/qingzhou.properties`，修改后，重启轻舟实例即可生效。

### 接口参考

| 接口URI        | 接口说明          | 传参形式                     |
|--------------|---------------|--------------------------|
| /invoke      | 执行指定应用的模块操作   | /invoke/-/admin/app/list |
| /web/welcome | 获取应用列表        | 无需参数                     |
| /web/app     | 获取某个应用的元数据    | /web/app?appId=admin@-   |
| /web/model   | 获取某个模块的元数据    | /web/model?modelId=app@admin@- |
| /register    | 注册远程实例上的应用    | 内部加密                     |
| /refresh     | 刷新远程实例的通信密钥   | 内部加密               |
| /agent       | 在远程实例上执行应用的操作 | 内部加密               |

### 常见问题

### 故障排除
