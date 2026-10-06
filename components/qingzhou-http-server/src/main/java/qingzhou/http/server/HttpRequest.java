package qingzhou.http.server;

import java.util.List;
import java.util.Map;

public interface HttpRequest {
    String getRemoteHost();

    String getPath();

    /**
     * 原始未解码的请求 URI（含 query）。
     * 仅可用于日志与展示：<b>禁止</b>用于路由或任何安全判断——它未做解码与规范化。
     */
    String getRawUri();

    /**
     * 取同名参数的第一个值。同名参数按「query 在前、表单体在后」合并，
     * 故该方法返回的是 query 中的值；需要区分来源时应自行拆分处理。
     */
    String getParameter(String name);

    /**
     * 全部参数（query 与表单体合并后的只读视图）。同名参数按「query 在前、表单体在后」排列。
     */
    Map<String, List<String>> getParameters();

    String getHeader(String header);

    String getContentType();

    String getMethod();

    /**
     * 本次连接的协议标识（http/https），由框架按实际连接给出。
     * 安全相关判定（Cookie 的 Secure、HSTS 等）须以此为准，不能依据配置字符串。
     */
    String getScheme();

    boolean isFormUrlencoded();

    /**
     * 请求体。返回内部数组，<b>调用方不得修改</b>其内容——参数解析缓存基于同一数组。
     */
    byte[] getBody();

    // 认证主体：由认证层写入，handler 只读；未认证时为 null
    String getPrincipal();

    // 认证角色：返回副本，handler 只读；未认证时为 null
    String[] getRoles();
}
