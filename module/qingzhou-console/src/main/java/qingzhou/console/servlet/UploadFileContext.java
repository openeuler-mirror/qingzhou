package qingzhou.console.servlet;

import java.io.IOException;
import java.io.InputStream;

public interface UploadFileContext {
    String uploadFileParam = "uploadFile";

    String getFileName();

    long getSize();

    InputStream getInputStream() throws IOException;
}
