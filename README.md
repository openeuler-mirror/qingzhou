# Qingzhou（轻舟）融合管理开发平台

## 项目简介

轻舟可用于实现各类业务系统的集中式、一致化管理。

通过在服务器部署轻舟代理，可自动识别业务系统类型并远程注册至轻舟管控台，进而实现集中式管理。

轻舟应用是基于轻舟 API 开发的插件化管理组件。
轻舟对业务系统的管控，依托轻舟代理调度对应轻舟应用完成，不同类型的业务系统需匹配专属轻舟应用。
轻舟 API 为各类业务系统提供统一的开发规范，最终实现多品类业务系统的标准化、一致化管理。

## 开始使用

### 快速入门

#### 编译打包

确保您已经安装了 JDK 1.8+ 和 Maven 3.8+。

在源码根目录执行 `mvn clean install -DskipTests`，执行完成后，
可在 `qingzhou/target/qingzhou` 得到轻舟的二进制产品安装包。
- bin: 可执行程序目录。
  - start.sh: Linux/Mac 平台启动脚本，可以“空格+实例名”指定要启动的实例（即 instances 目录下的子文件名），默认为 default。
  - start.bat: Windows 平台启动脚本，可以“空格+实例名”指定要启动的实例（即 instances 目录下的子文件名）），默认为 default。
  - gen-cipher-key.sh: 生成随机的对称加密密钥，用于 qingzhou.crypto.Crypto.getCipher(String key) 服务。
  - gen-pair-key.sh: 生成随机的非对称公钥和私钥对，用于 qingzhou.crypto.Crypto.getPairCipher(String publicKey, String
    privateKey) 服务。
- instances: 实例数据目录，可根据业务需求复制多份实例目录，如：开发、测试、OA、门户等。
  - default: 默认启动的实例目录。
- lib: 存放轻舟源码编译后的二进制 *.jar 文件。
  - version*.zip: 轻舟二进制 *.jar 文件的分发 zip 包，启动时会自动解压开，解压后此文件不再需要，可删除或继续留存。
  - version* 目录: 在启动时根据 version*.zip 自动解压生成的目录，此目录下的 *.jar 文件会加载到内存。
  - 重点说明：启动时，如果 version* 目录已存在，则会与 version*.zip 比对，不一致，则删除重新生成，若要禁止此行为，可将
    version*.zip 移出此目录，或修改其名字不要以 version 开头。

#### 启动服务

在 `${轻舟安装包}/bin` 下，执行 start 脚本，如：sh start.sh，等待启动完成。

#### 访问管控台

轻舟提供了可视化的 Web 管理控制台，可打开浏览器访问：

[http://localhost:7900/console](http://localhost:7900/console)

### 开发应用

📌 参考示例：`apps/qingzhou-demo-app`。

#### 新建项目

在 `qingzhou/apps` 目录下新建 Maven 子模块。

#### 编写应用入口类

轻舟启动时会调用此类，并传递 `AppContext` 接口供应用使用。
- 创建类，实现 `QingzhouApp` 接口。
- 添加 `@App`、`@Menu` 注解，配置元数据。
- 重写 `start()` 方法，定制启动逻辑。

#### 编写业务模块类

模块类可对应管控台上的一个功能菜单。
- 创建类，继承 `ModelBase`。
- 添加 `@Model` 注解，配置元数据。
- 根据业务需求，实现相应的能力接口。

| 接口        | 功能说明           |
|:----------|:---------------|
| `List`    | 该模块支持查看数据列表    |
| `Show`    | 该模块支持查看某条数据的详情 |
| `Add`     | 该模块支持新增数据      |
| `Update`  | 该模块支持编辑数据      |
| `Delete`  | 该模块支持删除数据      |
| `Monitor` | 该模块支持实时监控      |

#### 添加业务模块字段

模块字段可对应管控台上表单页面的一个字段。
- 创建属性，如 `public String id`。
- 添加 `@ModelField` 注解，配置元数据。

#### 添加业务模块操作

模块操作可对应一个 REST 接口，内置的能力接口会对应页面上的按钮。
- 创建方法，如 `public void myMethod(Request request){}`，注意方法的参数有且只能有一个 Request 类型。
- 添加 `@ModelAction` 注解，配置元数据。

4.4 **图标配置**

开发后端应用时，在 `@App`、`@Menu`、`@Model` 注解中可以配置图标，前端使用 **Element Plus Icons** 作为图标库，所有图标名称直接使用 Element Plus 官方图标名称（PascalCase 格式）。

#### 图标配置方式

##### 4.4.1 应用图标 (@App)

在 Java 后端使用 `@App` 注解配置应用图标：

```java
@App(name = "示例应用", icon = "Ship")
public class DemoApp extends QingzhouApp {
    // ...
}
```

##### 4.4.2 菜单图标 (@Menu)

在 Java 后端使用 `@Menu` 注解配置菜单分组图标：

```java
@Menus({
    @Menu(code = "basic", name = "基础功能", icon = "Grid"),
    @Menu(code = "advanced", name = "高级功能", icon = "Tools"),
    @Menu(code = "system", name = "系统", icon = "Setting")
})
```

##### 4.4.3 模型图标 (@Model)

在 Java 后端使用 `@Model` 注解配置模型图标：

```java
// 作为一级菜单的 Model（不配置 menu 属性）
@Model(code = "dashboard", name = "首页", icon = "HomeFilled")
public class Dashboard extends ModelBase {
    // ...
}

// 属于某个菜单分组的 Model
@Model(code = "student", name = "学生管理", icon = "User", menu = "basic")
public class Student extends ModelBase {
    // ...
}
```

#### 常用图标参考

| 图标名称 | 说明 | 适用场景 |
|---------|------|---------|
| `HomeFilled` / `Home` | 首页/主页 | 仪表盘、首页 |
| `Grid` | 网格/菜单 | 功能菜单分组 |
| `User` / `UserFilled` | 用户 | 用户管理、个人中心 |
| `Tools` | 工具 | 高级功能、设置 |
| `Setting` / `SettingFilled` | 设置 | 系统设置、配置 |
| `Document` / `DocumentChecked` | 文档 | 默认图标、文档管理 |
| `List` / `Memo` | 列表 | 数据列表、记录 |
| `Search` | 搜索 | 查询功能 |
| `Plus` / `CirclePlus` | 添加 | 新增操作 |
| `Edit` / `EditPen` | 编辑 | 编辑操作 |
| `Delete` / `DeleteFilled` | 删除 | 删除操作 |
| `View` / `ViewFilled` | 查看 | 详情展示 |
| `Refresh` | 刷新 | 数据刷新 |
| `Download` / `Upload` | 下载/上传 | 文件操作 |
| `Warning` / `WarningFilled` | 警告 | 提示信息 |
| `InfoFilled` / `Info` | 信息 | 帮助说明 |
| `SuccessFilled` / `CircleCheck` | 成功 | 成功状态 |
| `CircleClose` / `Close` | 关闭 | 关闭操作 |
| `ArrowRight` / `ArrowLeft` | 箭头 | 导航、展开 |
| `Menu` / `Expand` / `Fold` | 菜单 | 侧边栏控制 |
| `Monitor` | 监控 | 监控页面 |
| `DataLine` / `TrendCharts` | 数据 | 图表、统计 |
| `ShoppingCart` / `Goods` | 商品 | 产品、订单管理 |
| `Money` / `Wallet` | 金额 | 财务、支付 |
| `Calendar` / `Clock` | 时间 | 日程、历史 |
| `Message` / `ChatDotRound` | 消息 | 通知、留言 |
| `Lock` / `Unlock` | 锁 | 安全、权限 |
| `Star` / `StarFilled` | 收藏 | 收藏功能 |
| `Share` | 分享 | 分享操作 |
| `Printer` | 打印 | 打印功能 |
| `Ship` / `Truck` | 物流 | 运输、配送 |

#### 图标命名规范

1. **使用 PascalCase**：如 `HomeFilled`、`CirclePlus`
2. **避免使用 kebab-case**：如 `home-filled` 不正确
3. **完整图标列表参考**：[Element Plus Icons 官方文档](https://element-plus.org/zh-CN/component/icon.html#icon-collection)

#### 默认图标行为

- **应用图标**：未配置时显示默认图标 `Document`
- **菜单分组图标**：未配置时显示默认图标 `Folder`
- **模型图标**：未配置时显示默认图标 `Document`

5. **应用部署**
#### 应用部署

- 执行 `mvn clean install` 编译。
- 将生成的 jar 包放入`${轻舟安装包}/instances/default/apps` 目录下。
- 重启轻舟。
- 再次访问管控台，可看到部署的应用。

## 功能

### REST 接口

### 远程管理

## 更多信息

### 参数配置

参数配置文件在 `${轻舟安装包}/instances/default/conf/qingzhou.properties`，修改后，重启轻舟实例即可生效。

### 接口参考

| 接口URI        | 接口说明          | 传参形式                           |
|--------------|---------------|--------------------------------|
| /invoke      | 执行指定应用的模块操作   | /invoke/-/admin/app/list       |
| /register    | 注册远程实例上的应用    |                            |
| /refresh     | 刷新远程实例的通信密钥   |                            |
| /agent       | 在远程实例上执行应用的操作 |                            |
| /console     | 管控台前端静态资源     |                            |
| /web/welcome | 管控台后端应用概览     |                            |
| /web/app     | 管控台后端应用元数据    | /web/app?appId=admin@-         |
| /web/model   | 管控台后端应用模块元数据  | /web/model?modelId=app@admin@- |

### 常见问题

### 故障排除
