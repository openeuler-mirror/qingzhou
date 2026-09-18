package qingzhou.http.client;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public interface Request {
    Request method(HttpMethod method);

    Request header(String key, String val);

    Request headers(Map<String, String> headers);

    Request params(Map<String, String> params);

    Request body(byte[] body);

    Request files(Map<String, List<String>> files);

    /**
     * Sets a specified timeout value, in milliseconds,
     * to be used when opening a communications link to the resource referenced by this URLConnection.
     * If the timeout expires before the connection can be established, a java.net.SocketTimeoutException is raised.
     * A timeout of zero is interpreted as an infinite timeout.
     */
    Request connectTimeout(int connectTimeout);

    /**
     * Sets the read timeout to a specified timeout, in milliseconds.
     * A non-zero value specifies the timeout when reading from Input stream when a connection is established to a resource.
     * If the timeout expires before there is data available for read, a java.net.SocketTimeoutException is raised.
     * A timeout of zero is interpreted as an infinite timeout.
     */
    Request readTimeout(int readTimeout);

    /**
     * 指定本次请求信任的服务端证书。
     * 指定后以所给证书为信任锚校验服务端证书，并校验主机名；未指定时使用 JVM 默认 CA 信任库。
     */
    Request trustedCertificates(X509Certificate... certificates);

    /**
     * 信任所有证书且不校验主机名。存在中间人攻击风险，仅限测试或自签名内网场景。
     */
    Request trustAllCertificates();

    /**
     * 非流式响应体大小上限（字节），超出时抛 IOException。0 表示不限制，默认 64MB。
     */
    Request maxBodySize(int maxBodySize);
}
