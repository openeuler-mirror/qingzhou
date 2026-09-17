package qingzhou.http.client;

import java.security.cert.X509Certificate;
import java.util.Map;

public interface Request {
    Request method(HttpMethod method);

    Request header(String key, String val);

    Request headers(Map<String, String> headers);

    Request params(Map<String, String> params);

    Request body(byte[] body);

    Request files(Map<String, String> files);

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
     * 指定本次请求信任的服务端证书，用于自签证书场景。
     * 未指定时信任所有证书且不校验主机名；指定后以所给证书为信任锚校验服务端证书，并校验主机名。
     */
    Request trustedCertificates(X509Certificate... certificates);
}
