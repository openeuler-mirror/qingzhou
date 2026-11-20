# API 索引（qingzhou-api 1.0.0-SNAPSHOT）

本索引只用于快速定位，签名以 `references/api/qingzhou/api/` 下源码为准。索引与源码不一致时，以源码为准并修正生成代码。

## qingzhou.api

| 类型 | 成员 |
|---|---|
| `QingzhouApp`（接口） | `void start(AppContext) throws Exception`；`default void stop()` |
| `AppContext`（接口） | `Map<String,String> getAppConfig()`；`String getDetectedPath()`；`File getRoot()`；`File getTempDir()`；`<T> T getService(Class<T>)`；`<T> T getService(Class<T>, String name)`；`void addActionFilter(ActionFilter...)` |
| `ModelBase`（抽象类） | `AppContext getAppContext()`；`Request getCurrentRequest()`；`void start()`；`void stop()`；`setAppContext` / `setCurrentRequest` / `clearCurrentRequest` 为框架注入与清理入口，应用不得调用 |
| `Request`（接口） | `Response getResponse()`；`String getModel()`；`String getAction()`；`String getId()`；`String getParameter(String)`；`Map<String,String> getParameters()` |
| `Response`（接口） | `success(boolean)`、`data(Object)`、`message(String)`、`messageLevel(MessageLevel)`、`status(int)`、`contentType(String)`、`header(String,String)`、`error(String)`，均返回 `Response`；内部枚举 `MessageLevel {info, warn, error}` |
| `ActionFilter`（接口） | `void doFilter(Request, FilterChain) throws Throwable` |
| `FilterChain`（接口） | `void doFilter() throws Throwable` |
| `InputType`（枚举） | `text, textarea, password, number, decimal, datetime, range_datetime, file, bool, radio, checkbox, sortable_checkbox, select, multiselect, sortable, kv, markdown, combine, grouped_multiselect` |
| `FieldType`（枚举） | `form, monitor` |
| `ChartType`（枚举） | `line, bar, gauge, stat, pie` |

## qingzhou.api.action

| 类型 | 成员 | 动作 code |
|---|---|---|
| `Page` | `List<String[]> page(int, int, Map<String,String>, String[]) throws Exception`；`default int total(Map<String,String>)`（-1 不分页）；`boolean contains(String)` | `page` |
| `Show` | `Map<String,String> show(String id) throws Exception` | `show` |
| `Add` | `void add(Map<String,String> data) throws Exception` | `add` |
| `Update` | `void update(String id, Map<String,String> data) throws Exception` | `update` |
| `Delete` | `void delete(String id) throws Exception` | `delete` |
| `Monitor` | `Map<String,String> monitor(String id) throws Exception` | `monitor` |
| `Download` | `File file(String id) throws Exception` | `file`、`download` |
| `Namespace` | `void namespace(String id) throws Exception`；`String current()` | `namespace`、`current` |

易错点：

- 总数方法是 `Page.total(...)`，**没有** `totalSize`；`total` 与 `contains` 均未声明受检异常。
- `AppContext` **没有** `getPid()`；应用配置统一走 `getAppConfig()`，不是 `getProperties()`。
- `Add`/`Update` 的 `data` 只含本次提交且 `add`/`update` 为真的字段；`Update` 已剔除 `id`。

## qingzhou.api.annotations

保留策略均为 `CLASS`，构建期提取为 `QINGZHOU-INF` 元数据。

| 注解 | 目标 | 成员（默认值） |
|---|---|---|
| `@App` | TYPE | `code("")`、`roles("")`、`icon("")`、`name()`、`info({})` |
| `@Menu` | TYPE，可重复（容器 `@Menus`） | `name()`、`code()`、`icon("")`、`order(9)`、`parent("")` |
| `@I18n` | TYPE，可重复（容器 `@I18ns`） | `code()`、`name()` |
| `@Model` | TYPE | `code("")`、`roles("")`、`icon("")`、`menu("")`、`order(9)`、`name()`、`info({})` |
| `@ModelField` | FIELD | `code("")`、`field_type(form)`、`input_type(text)`、`display("")`、`ref_model("")`、`link_to("")`、`options({})`、`separator(",")`、`id(false)`、`readonly(false)`、`required(false)`、`min_value(Long.MIN_VALUE)`、`max_value(Long.MAX_VALUE)`、`min_length(-1)`、`max_length(Integer.MAX_VALUE)`、`host(false)`、`port(false)`、`email(false)`、`file(false)`、`pattern("")`、`add(true)`、`update(true)`、`show(true)`、`list(false)`、`search(false)`、`numeric(false)`、`chart_type(line)`、`group("")`、`chart_title("")`、`color({})`、`width_percent(-1)`、`name()`、`info({})` |
| `@ModelAction` | METHOD、TYPE，可重复（容器 `@OverrideDefaultActions`） | `code("")`、`roles("")`、`icon("")`、`order(9)`、`display("")`、`add(false)`、`update(false)`、`show(false)`、`list_head(false)`、`list(false)`、`batch(false)`、`name({})`、`info({})`、`confirm({})` |

## 平台共享服务（不在本包源码内）

| 服务 | 获取方式 | 允许使用的方法 |
|---|---|---|
| `qingzhou.json.Json` | `getAppContext().getService(Json.class)` | `String toJson(Object) throws Exception`；`<T> T fromJson(String, Class<T>) throws Exception` |
| `qingzhou.logger.Logger` | `getAppContext().getService(Logger.class)` | 日志输出；不得打印密码、Token 等敏感信息 |

两个服务的 jar 均由平台提供，编译依赖为 `provided`：`qingzhou:qingzhou-api`、`qingzhou:qingzhou-json`。
