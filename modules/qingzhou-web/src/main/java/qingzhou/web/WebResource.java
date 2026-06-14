package qingzhou.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.MessageDigest;

class WebResource {
    private final MessageDigest messageDigest;
    private final Base16Coder base16Coder;
    private final URL url;

    private volatile String etag = null;
    private volatile byte[] content = null; // 内容缓存，避免每次请求重新读取流

    WebResource(MessageDigest md, Base16Coder base16Coder, URL url) {
        this.messageDigest = md;
        this.base16Coder = base16Coder;
        this.url = url;
    }

    String getETag() throws Exception {
        if (etag == null) {
            synchronized (this) {
                if (etag == null) {
                    byte[] md5Bytes = messageDigest.md5(getContent());
                    String md5Str = base16Coder.encode(md5Bytes);
                    etag = "\"" + md5Str + "\"";
                }
            }
        }
        return etag;
    }

    byte[] getContent() throws IOException {
        if (content == null) {
            synchronized (this) {
                if (content == null) {
                    content = readContent();
                }
            }
        }
        return content;
    }

    private byte[] readContent() throws IOException {
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
}
