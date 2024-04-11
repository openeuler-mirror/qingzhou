package qingzhou.http;

import java.io.IOException;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    /**
     * Handles a given request and generates an appropriate response.
     * See {@link HttpExchange} for a description of the steps
     * involved in handling an exchange. Container invokes this method
     * when it receives an incoming request.
     *
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException when an I/O error happens during request
     *                     handling
     */
    void handle(HttpExchange exchange) throws IOException;
}
