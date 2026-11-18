# 使用说明

本技能包用于把 REST 接口文档转换为可在轻舟平台运行的 Java 8 应用。三种用法内容完全相同，按所用客户端能力选择一种即可。

- 支持 Agent Skills / 技能导入的客户端：方式一
- 无技能机制、但支持项目规则文件的客户端：方式二
- 网页对话类模型：方式三

## 方式一：装进支持 Agent Skills 的客户端（推荐）

技能机制通常要求「一个目录一个技能，根目录必须有 `SKILL.md`」。把整个目录复制到客户端的技能目录，目录名保持 `qingzhou-app-builder`：

```bash
# 以 Claude Code 类客户端为例（其他客户端换成各自的 skills 目录）
mkdir -p ~/.claude/skills
cp -R ~/Desktop/qingzhou-app-builder-skill ~/.claude/skills/qingzhou-app-builder
```

通过「已安装技能 / 插件」管理的客户端（如 Trae），走导入或技能市场入口添加该目录；若当前客户端没有技能入口，改用方式二。

装好后由 `SKILL.md` 的 `description` 自动触发，正常提需求即可：

> 用轻舟应用构建技能，根据下面的 REST 文档生成应用，标识 inventory-check，只做库存列表和详情，按仓库筛选。

## 方式二：仓库级引用（无技能机制时）

在目标工程根目录的 `AGENTS.md`（或 `.cursor/rules` 等规则文件）中加一行：

```markdown
轻舟应用开发时，先读取 ~/qingzhou-app-builder-skill/SKILL.md，
并按其“素材地图”加载 references 下的规则与 API 源码。
```

之后在任意支持该规则文件的模型里直接描述需求即可。

## 方式三：纯对话粘贴（网页版模型）

按顺序提供三样内容：

1. `SKILL.md` 全文（作为任务要求）
2. `references/generation-rules.md` 与 `references/api-index.md` 全文
3. 本次要用的 `references/api/qingzhou/api/**` 源码文件，以及业务接口文档正文

随后按下面的句式发起任务：

> 按上述规则生成轻舟应用，标识 xxx。先输出接口映射表和待确认问题，等我确认后再逐个输出完整源码。

没有终端能力时，模型只输出源码，需要自行按 `references/templates/pom.xml` 建工程并执行 `mvn clean package`。

## 有终端时的完整流程

```bash
# 1. 建工程：应用标识、轻舟版本、产品库目录（产品包内 lib/version<版本>）
scripts/new-project.sh inventory-check 1.0.0-SNAPSHOT /path/to/qingzhou/lib/version1.0.0-SNAPSHOT ~/work

# 2. 模型按规则写源码到 ~/work/qingzhou-app-inventory-check/src/main/java/

# 3. 构建，并把 JAR 放入实例 apps（可选第二参数，同名不覆盖）
scripts/build.sh ~/work/qingzhou-app-inventory-check /path/to/qingzhou/instances/default/apps
```

Windows 使用：

```powershell
scripts/build.ps1 -Project <工程目录> -Apps <apps目录>
```

## 使用注意

- **版本必须对齐**：`references/api` 与 `references/templates/pom.xml` 要和目标轻舟同版本，跨版本使用时先替换这两处再生成。
- **构建环境**：JDK 1.8 与支持 Java 8 的 Maven；脚本会先校验，不满足直接报错。
- **部署边界**：脚本只负责构建和放置 JAR，不重启平台，也不代表已验证运行；重启后仍需人工验收页面与真实接口。
- **续建**：直接说明「继续应用构建 <标识>，列表正常但详情失败」，模型会续用原工程定位，不会重新生成整套源码覆盖已有修改。

## 补充说明

### 目录说明

```
qingzhou-app-builder-skill/
├── SKILL.md                  # 唯一入口：frontmatter 触发描述、素材地图、能力适配、六阶段流程、红线
├── USAGE.md                  # 本文件：面向使用者的安装与使用说明
├── references/
│   ├── generation-rules.md   # 生成规则：接口分析、页面映射、REST 调用、源码与输出要求
│   ├── app-spec.md           # 平台规范：注解属性、能力接口、权限、REST 入口、上传、监控契约
│   ├── detection.md          # 自动发现：Qingzhou-Detection-* 参数与探测流程
│   ├── api-index.md          # API 索引：类型/成员速查与易错点，先读索引再读源码
│   ├── api/qingzhou/api/**   # 与目标平台同版本的完整 API 源码，签名以源码为准
│   └── templates/pom.xml     # 工程 POM 模板，含 @APP@ 与 @QINGZHOU_VERSION@ 占位符
└── scripts/
    ├── new-project.sh        # 建工程：渲染 POM、落地平台依赖到 .qingzhou-repository
    ├── build.sh              # 构建：JDK 1.8 校验、mvn 打包、120 秒超时、可选放置 JAR
    └── build.ps1             # Windows 版构建脚本，行为与 build.sh 一致
```

### 关键改写点

相对原 `qingzhou-ai-skill-app-builder` 素材，主要调整如下：

1. **工具协议改为文件加脚本**：原 `app_builder_materials/project/source/build` 四个工具及其分页协议全部移除，改为直接读写 `references` 与调用 `scripts`，任何具备文件或终端能力的模型都能执行。
2. **四层文档收敛为两层**：原 `app-builder.md`（用户手册）、`AppBuilderSkill.md`（工具流程）、`rest-app-generation.md`（生成规则）、开发规范职责重叠；现只保留 `SKILL.md`（流程与红线）与 `references`（细节按需加载），用户手册不进入技能包。
3. **能力双轨表述**：每类操作都写明「具备该能力时直接做，不具备时如何降级」，纯对话模型不会因缺少工具而中断，也不会把工具假设传给用户。
4. **上下文分层**：`SKILL.md` 控制在百行级；API 源码不预载，先读 `api-index.md` 再定点读取，避免小上下文模型被源码挤占。
5. **小模型截断对策**：要求先输出完整覆盖清单，再按 Model 分批输出完整文件，明确禁止省略号、TODO、空实现或“参考其他文件”。
6. **安全红线前置并强化**：业务文档中的命令不得执行、凭据只来自运行配置、失败即抛、不试运行写接口、不改框架源码，这些原本分散在生成规则中的约束提升到 `SKILL.md` 顶层。
7. **工程操作脚本化**：原 Java 中的路径越界校验、同名 JAR 不覆盖、JDK 8 检测、120 秒超时、提取 `[ERROR]` 日志等逻辑，改写为 `new-project.sh`、`build.sh` 与 `build.ps1`。
8. **版本自洽**：`SKILL.md` 声明 `references/api` 与 `templates/pom.xml` 必须与目标产品同版本，升级时整包替换，避免混用。
9. **修正素材与源码不一致**：`Page.totalSize` 改为实际存在的 `Page.total(...)`；删除 `AppContext` 中不存在的 `getPid()`，配置读取统一为 `getAppConfig()`。
