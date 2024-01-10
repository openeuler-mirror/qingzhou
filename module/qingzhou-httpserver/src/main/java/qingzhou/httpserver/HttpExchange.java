package qingzhou.httpserver;

import java.io.InputStream;
import java.io.OutputStream;

public interface HttpExchange {
    InputStream getRequestBody();

    OutputStream getResponseBody();
}
