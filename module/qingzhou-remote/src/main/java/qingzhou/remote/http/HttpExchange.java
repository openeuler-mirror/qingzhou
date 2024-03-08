package qingzhou.remote.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class encapsulates an HTTP request received and a
 * response to be generated in one exchange. It provides methods
 * for examining the request from the client, and for building and
 * sending the response.
 * <p>
 * A <code>HttpExchange</code> must be closed to free or reuse
 * underlying resources. The effect of failing to close an exchange
 * is undefined.
 *
 * @author Jitendra Kotamraju
 * @since JAX-WS 2.2
 */
public interface HttpExchange {
    /**
     * Returns the part of the request's URI from the protocol
     * name up to the query string in the first line of the HTTP request.
     * Container doesn't decode this string.
     *
     * @return the request URI
     */
    String getRequestURI();

    /**
     * Returns a stream from which the request body can be read.
     * Multiple calls to this method will return the same stream.
     *
     * @return the stream from which the request body can be read.
     * @throws IOException if any i/o error during request processing
     */
    InputStream getRequestBody() throws IOException;

    /**
     * Sets the HTTP status code for the response.
     *
     * <p>
     * This method must be called prior to calling {@link #getResponseBody}.
     *
     * @param status the response code to send
     * @see #getResponseBody
     */
    void setStatus(int status) throws IOException;

    /**
     * Adds a response header with the given name and value. This method
     * allows a response header to have multiple values. This is a
     * convenience method to add a response header.
     *
     * @param name  the name of the header
     * @param value the additional header value. If it contains octet string,
     *              it should be encoded according to
     *              RFC 2047 (<a href="http://www.ietf.org/rfc/rfc2047.txt">...</a>)
     */
    void addResponseHeader(String name, String value);

    /**
     * Returns a stream to which the response body must be
     * written. {@link #setStatus}) must be called prior to calling
     * this method. Multiple calls to this method (for the same exchange)
     * will return the same stream.
     *
     * @return the stream to which the response body is written
     * @throws IOException if any i/o error during response processing
     */
    OutputStream getResponseBody() throws IOException;

    /**
     * This must be called to end an exchange. Container takes care of
     * closing request and response streams. This must be called so that
     * the container can free or reuse underlying resources.
     *
     * @throws IOException if any i/o error
     */
    void close() throws IOException;
}
