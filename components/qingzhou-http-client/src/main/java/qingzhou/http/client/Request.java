package qingzhou.http.client;

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
}
