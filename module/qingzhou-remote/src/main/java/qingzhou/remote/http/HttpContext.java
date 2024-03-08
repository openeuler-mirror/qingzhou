package qingzhou.remote.http;

/**
 * HttpContext represents a mapping between the root URI path of a web
 * service to a {@link HttpHandler} which is invoked to handle requests
 * destined for that path on the associated container.
 * <p>
 * Container provides the implementation for this, and it matches
 * web service requests to corresponding HttpContext objects.
 */
public interface HttpContext {
    /**
     * JAX-WS runtime sets its handler to handle
     * HTTP requests for this context. Container or its extensions
     * use this handler to process the requests.
     *
     * @param handler the handler to set for this context
     */
    void setHandler(HttpHandler handler);

    /**
     * Returns the path for this context. This path uniquely identifies
     * an endpoint inside an application and the path is relative to
     * application's context path. Container should give this
     * path based on how it matches request URIs to this HttpContext object.
     *
     * <p>
     * For servlet container, this is typically an url-pattern for an endpoint.
     *
     * <p>
     * Endpoint's address for this context can be computed as follows:
     * <pre>
     *  HttpExchange exchange = ...;
     *  String endpointAddress =
     *      exchange.getScheme() + "://"
     *      + exchange.getLocalAddress().getHostName()
     *      + ":" + exchange.getLocalAddress().getPort()
     *      + exchange.getContextPath() + getPath();
     * </pre>
     *
     * @return this context's path
     */
    String getPath();
}
