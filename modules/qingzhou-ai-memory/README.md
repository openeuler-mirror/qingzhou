# qingzhou-ai-memory

对话记忆实现模块。上下文读写接口 `ChatMemory` 位于 `qingzhou-llm`（对齐业界记忆抽象：
加载历史 + 消息落库，ChatModel 自动调用）；会话治理接口 `ChatMemoryAdmin` 定义在本模块
（列表/删除/改名属管理面业务，业界均不放入通用 LLM 抽象）。两个接口由同一实现类承载，
`ChatMemoryProvider` 同时以两个 OSGi 服务注册。消费方（如 `qingzhou-ai` 的 `AiChat`）
仅依赖接口，通过 OSGi 服务动态引用，未部署本模块时对话自然退化为单轮。

## 命名约定

记忆相关的接口、实现、字段、变量统一使用 `ChatMemory` / `memory` 命名（不再使用
`ChatMemoryStore` / `memoryStore` 旧名），保持名实一致：

| 元素 | 名称 | 所在 |
|---|---|---|
| 上下文读写接口 | `ChatMemory` | qingzhou-llm |
| 会话治理接口 | `ChatMemoryAdmin` | 本模块 |
| 会话摘要 DTO | `ConversationSummary` | 本模块 |
| 文件实现 | `FileChatMemory` | 本模块 |
| 内存实现 | `InMemoryChatMemory` | 本模块 |
| OSGi 装配组件 | `ChatMemoryProvider` | 本模块 |
| 消费方引用字段 | `memory` | qingzhou-ai |

## 配置

`qingzhou.properties` 中显式配置 `qingzhou-ai-memory.type` 才启用记忆（缺省/非法值安静降级）：

```properties
qingzhou-ai-memory.type = file    # file：实例目录 data/ai-conversations 持久化；memory：纯内存
```

修改配置后重启实例生效。

## 接口职责

- **对话路径（`ChatMemory`，ChatModel 自动调用）**：`resolveConversationId`（会话标识采信/生成）、
  `recentHistory`（截断的上下文历史）、`appendUserMessage` / `appendAssistantMessage`（消息落库）。
- **治理路径（`ChatMemoryAdmin`，管理面/HTTP 直接调用）**：`listConversations`（摘要列表，按最近更新倒序）、
  `listMessages`（会话全量消息，区别于 `recentHistory` 的上下文截断）、
  `deleteConversation` / `renameConversation`（删除/改名，会话不存在或 id 非法返回 `false`，供 HTTP 层区分 404）。

## 安全约定

- `userId` 一律来自服务端鉴权解析（token），不接受前端传参；实现按用户隔离存储并校验归属，防横向越权。
- 会话文件目录名为 userId 的 SHA-256 摘要；治理方法入参的 `conversationId` 先过格式校验（`^[A-Za-z0-9_-]{8,64}$`）防路径遍历。
- 容量上限：单会话 200 条、单用户 50 会话、单条 8000 字符（超出截断/淘汰最旧）。

## 构建

```bash
mvn -pl modules/qingzhou-ai-memory test
```
