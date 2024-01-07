package qingzhou.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface HttpExchange {
    /**
     * Returns the name of the scheme used to make this request,
     * for example: http, or https.
     */
    String getScheme();

    /**
     * Returns the part of the request's URI from the protocol
     * name up to the query string in the first line of the HTTP request.
     */
    String getRequestURI();

    /**
     * Returns the context path of all the endpoints in an application.
     * This path is the portion of the request URI that indicates the
     * context of the request. The context path always comes first in a
     * request URI. The path starts with a "/" character but does not
     * end with a "/" character. If this method returns "", the request
     * is for default context. The container does not decode this string.
     */
    String getContextPath();

    /**
     * Returns the extra path information that follows the web service
     * path but precedes the query string in the request URI and will start
     * with a "/" character.
     */
    String getPathInfo();

    /**
     * Returns the query string that is contained in the request URI
     * after the path.
     */
    String getQueryString();

    String getRequestMethod();

    Map<String, List<String>> getRequestHeaders();

    String getRequestHeader(String name);

    InputStream getRequestBody() throws IOException;

    OutputStream getResponseBody() throws IOException;

    Map<String, List<String>> getResponseHeaders();

    void addResponseHeader(String name, String value);

    void setStatus(int status);

    void close() throws IOException;
}
