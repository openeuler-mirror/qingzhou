package qingzhou.console.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RequestContext {
    String getRequestURI();

    String[] getParameterValues(String name);

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    UploadFileContext getUploadContext() throws Exception;
}
