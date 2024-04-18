package qingzhou.console.jmx;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JmxHttpServletResponse implements HttpServletResponse {

    private ByteArrayServletOutputStream outputStream = new ByteArrayServletOutputStream();
    private int status;
    private final Map<String, String> header = new HashMap<>();
    private boolean committed = false;

    private PrintWriter writer;

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public boolean containsHeader(String s) {
        return header.containsKey(s);
    }

    @Override
    public String encodeURL(String s) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String s) {
        return null;
    }

    @Override
    public String encodeUrl(String s) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return null;
    }

    @Override
    public void sendError(int i, String s) {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot call sendError() after the response has been committed");
        }
        this.committed = true;
        this.status = i;
    }

    @Override
    public void sendError(int i) {
        sendError(i, null);
    }

    @Override
    public void sendRedirect(String s) {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot call sendRedirect() after the response has been committed");
        }
        this.committed = true;
    }

    @Override
    public void setDateHeader(String s, long l) {
    }

    @Override
    public void addDateHeader(String s, long l) {
    }

    @Override
    public void setHeader(String s, String s1) {
        header.put(s, s1);
    }

    @Override
    public void addHeader(String s, String s1) {
        header.put(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
    }

    @Override
    public void addIntHeader(String s, int i) {
    }

    @Override
    public void setStatus(int i, String s) {
        if (isCommitted()) {
            return;
        }
        this.status = i;
        this.committed = true;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(int i) {
        setStatus(i, null);
    }

    @Override
    public String getHeader(String s) {
        return header.get(s);
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return header.values();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return header.keySet();
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) {
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void setContentType(String s) {
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(outputStream);
        }
        return writer;
    }

    @Override
    public void setContentLength(int i) {
    }

    @Override
    public void setContentLengthLong(long l) {
    }

    @Override
    public int getBufferSize() {
        return outputStream.toByteArray().length;
    }

    @Override
    public void setBufferSize(int i) {
    }

    @Override
    public void flushBuffer() {
        this.committed = true;
        writer.flush();
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot reset buffer after response has been committed");
        }
        outputStream = new ByteArrayServletOutputStream();
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot call reset() after response has been committed");
        }
        outputStream = new ByteArrayServletOutputStream();
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public void setLocale(Locale locale) {
    }

    public String getResult() {
        if (writer != null) {
            writer.flush();
        }
        return new String(outputStream.toByteArray());
    }

    private static class ByteArrayServletOutputStream extends ServletOutputStream {

        protected final ByteArrayOutputStream buf;

        public ByteArrayServletOutputStream() {
            this.buf = new ByteArrayOutputStream();
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener listener) {

        }

        @Override
        public void write(int b) {
            buf.write(b);
        }

        public byte[] toByteArray() {
            return buf.toByteArray();
        }
    }
}