package qingzhou.config.remote;

/**
 * 远程配置中心访问异常：统一包装网络/鉴权/协议等失败，
 * message 只描述原因，不含口令、token 等敏感信息。
 */
public class RemoteConfigException extends RuntimeException {
    public RemoteConfigException(String message) {
        super(message);
    }

    public RemoteConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
