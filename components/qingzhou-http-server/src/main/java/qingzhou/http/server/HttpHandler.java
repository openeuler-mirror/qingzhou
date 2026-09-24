package qingzhou.http.server;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";
    String HANDLE_NO_AUTH = "HANDLE_NO_AUTH";

    void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception;

    /**
     * Handler 级认证器，优先级高于系统级 {@link Authenticator}：
     * 返回 {@link AuthResult#pass} 时系统级认证器不再执行；返回 {@link AuthResult#reject} 直接以 401 结束请求；
     * 返回 {@link AuthResult#abstain()} 或 null 表示弃权，转交系统级认证器。
     * <p>
     * 抛出的任何 {@link Throwable}（含 OSGi 刷新 bundle 时的 {@link NoClassDefFoundError}）一律按拒绝处理。
     * 注意：{@code noAuth} 注册方式只豁免系统级认证器，本认证器返回 reject 时仍然拒绝请求。
     */
    default HandlerAuthenticator customAuthenticator() {
        return null;
    }

    default StreamHandler multipartStreamHandler() {
        return null;
    }

    interface StreamHandler {
        void onBegin(HttpRequest request, HttpResponse response) throws Throwable;

        void onNext(byte[] data) throws Throwable;

        void onError(Throwable t);

        void onComplete();
    }
}
