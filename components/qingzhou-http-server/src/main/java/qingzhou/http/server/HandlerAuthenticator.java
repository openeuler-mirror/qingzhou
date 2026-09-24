package qingzhou.http.server;

/**
 * Handler 级认证器：仅对 {@link HttpHandler#customAuthenticator()} 所属的路径生效。
 * <p>
 * 与系统级 {@link Authenticator} 刻意拆成两个类型，尽管方法签名相同：系统级认证器由框架以
 * OSGi 服务方式收集并对所有路径生效，若二者同型，handler 级认证器一旦被标注为 {@code @Component}
 * 就会被误收集成系统级认证器，专用令牌随之升级为全站令牌。拆型后该错误在编译期即暴露。
 */
public interface HandlerAuthenticator {
    AuthResult authenticate(HttpRequest request);
}
