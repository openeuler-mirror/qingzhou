package qingzhou.api;

/**
 * 响应构造器，链式调用，最终序列化为 {@code {"success","data","msg","msgLevel"}}。
 * <p>
 * 动作既未写 {@link #data(Object)} 也未写 {@link #message(String)} 时，平台判定动作未执行并返回 404，
 * 以此避免"静默成功"；{@link #status(int)}、{@link #contentType(String)}、{@link #header(String, String)}
 * 供文件下载等非 JSON 响应使用。
 */
public interface Response {
    Response success(boolean success);

    /**
     * 设置返回数据。框架会按字段元数据裁剪：{@code show} 只保留 {@code show=true} 的字段，
     * {@code page} 按 {@code list=true} 的列逐位映射，{@code monitor} 只保留 {@code field_type=monitor} 的字段。
     * 若动作已自行调用本方法，框架不再覆盖。
     */
    Response data(Object data);

    Response message(String message);

    Response messageLevel(MessageLevel messageLevel);

    /**
     * HTTP 状态码，仅大于 0 时生效，缺省 200。
     */
    Response status(int status);

    /**
     * 响应 MIME 类型，缺省 {@code application/json;charset=utf-8}。
     */
    Response contentType(String contentType);

    /**
     * 追加响应头。{@code content-length}、{@code transfer-encoding}、{@code connection}、
     * {@code keep-alive}、{@code content-type}、{@code set-cookie*} 由平台掌控，设置无效。
     */
    Response header(String name, String value);

    /**
     * 失败响应，等价于:
     * this.message(error);
     * this.messageLevel(Response.MessageLevel.error);
     * this.success(false);
     * <p>
     * 未显式设置 {@link #status(int)} 时联动 400，避免出现"200 + success=false"。
     */
    Response error(String error);

    /**
     * 提示级别，决定前端以何种样式呈现 {@link #message(String)}。
     */
    enum MessageLevel {
        /**
         * 普通提示。
         */
        info,
        /**
         * 警告，操作未失败但需要用户注意。
         */
        warn,
        /**
         * 错误，通常与 {@code success=false} 配合。
         */
        error
    }
}
