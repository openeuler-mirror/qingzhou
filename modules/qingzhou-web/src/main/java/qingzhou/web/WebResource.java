package qingzhou.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;

public class WebResource {

    private final String path;
    private final URL url;
    private volatile String etag = null;

    public WebResource(String path, URL url) {
        this.path = path;
        this.url = url;
    }

    public String getETag() throws Exception {
        if (etag == null) {
            synchronized (this) {
                if (etag == null) {
                    etag = "\"" + md5(getContent()) + "\"";
                }
            }
        }
        return etag;
    }

    public byte[] getContent() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream in = url.openStream()) {
            byte[] buffer = new byte[1024 * 8];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        return os.toByteArray();
    }

    public String getPath() {
        return path;
    }

    private static String md5(byte[] body) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(body);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
