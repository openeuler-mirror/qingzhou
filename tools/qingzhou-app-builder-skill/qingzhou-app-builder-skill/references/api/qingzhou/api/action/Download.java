package qingzhou.api.action;

import java.io.File;

/**
 * 文件下载的能力，对应内置动作 {@code file}（列出可下载项）与 {@code download}（分块拉取内容）。
 * <p>
 * 应用只需给出基准目录或文件，列目录、多文件打包、断点续传与临时文件清理均由框架完成。
 */
public interface Download {
    /**
     * 返回下载基准：文件本身，或其父目录。
     * <p>
     * 返回目录时框架只列直接子文件（不递归，忽略以 {@code .} 开头的隐藏文件）；下载时要求目标文件解析后的
     * 规范路径仍落在该基准内，防止符号链接绕过。{@code id} 来自 URL 路径段会被拼进该基准，含
     * {@code ..}、{@code /}、{@code \} 时框架直接拒绝；应用仍须自行校验 {@code id} 的归属，
     * 避免越权访问他人的目录。
     */
    File file(String id) throws Exception;
}
