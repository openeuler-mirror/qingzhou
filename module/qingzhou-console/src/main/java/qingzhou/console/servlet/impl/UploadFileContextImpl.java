package qingzhou.console.servlet.impl;

import qingzhou.console.servlet.UploadFileContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

public class UploadFileContextImpl implements UploadFileContext {
    private final Part part;

    public UploadFileContextImpl(HttpServletRequest request) throws Exception {
        part = request.getPart(uploadFileParam);
    }

    @Override
    public String getFileName() {
        return part.getSubmittedFileName();
    }

    @Override
    public long getSize() {
        return part.getSize();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }
}
