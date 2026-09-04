package qingzhou.llm;

/**
 * 生成过程的阶段标识，随 STATUS 事件下发，客户端据此把笼统的“等待中”替换成具体文案。
 * <p>
 * 取值变更需同步前端：src/types/chat.ts 的 StageCode，以及 useChat.ts 的
 * STAGE_PHASE / STAGE_TEXT 两张映射表（前端表是穷举的，漏改会编译报错）。
 */
public enum ChatStage {
    /** 正在做技能匹配等前置工作（同步 LLM 调用，期间不会有任何内容事件） */
    matching,

    /** 仍在处理中：空闲心跳，用于让客户端区分“模型慢”与“连接断了” */
    working
}
