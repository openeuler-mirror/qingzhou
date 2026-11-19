package qingzhou.api;

import java.util.Map;

/**
 * 一次动作调用的入参视图，由平台在 HTTP 边界解析后构造，应用侧只读。
 * <p>
 * 对应统一入口 {@code /registry/invoke/{instance}/{app}/{model}/{action}/{id}}；同一套请求同时服务于管控台、
 * AI 工具与监控采集，应用不要假设调用方做过参数预处理。
 */
public interface Request {
    /**
     * 本次调用的响应对象。
     */
    Response getResponse();

    /**
     * 请求路径中的模块标识，即 {@code @Model.code}。
     */
    String getModel();

    /**
     * 请求路径中的动作标识，即内置动作名或 {@code @ModelAction.code}。
     */
    String getAction();

    /**
     * 请求路径中的记录标识；列表、新增等动作可能为空。
     */
    String getId();

    /**
     * 取单个参数值，URL query 与 POST body（表单或 JSON）已合并；同名多值只取第一个。
     *
     * @param name 字段 code
     */
    String getParameter(String name);

    /**
     * 全部参数的单值只读视图。参数由平台在输入边界一次性解析（如上传字段已替换为服务端临时路径），
     * 应用侧回写会绕过配额与校验，故此处不提供写入口。
     */
    Map<String, String> getParameters();
}
