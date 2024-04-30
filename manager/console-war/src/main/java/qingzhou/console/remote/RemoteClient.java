package qingzhou.console.remote;

import qingzhou.api.Request;
import qingzhou.console.ResponseImpl;
import qingzhou.console.controller.SystemController;
import qingzhou.console.util.StringUtil;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.engine.util.FileUtil;
import qingzhou.json.Json;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class RemoteClient {
    private static SSLSocketFactory ssf;
    public static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManagerInternal();

    public static ResponseImpl sendReq(String url, Request request, String remoteKey) throws Exception {
        try {
            return sendReq0(url, request, remoteKey);
        } catch (Exception e) {
            if (e instanceof SSLException
                    && e.getMessage() != null
                    && e.getMessage().contains("Unrecognized SSL message")) {
                url = "http" + url.substring(5);
                return sendReq0(url, request, remoteKey);
            } else {
                throw e;
            }
        }
    }

    private static ResponseImpl sendReq0(String url, Request request, String remoteKey) throws Exception {
        HttpURLConnection connection = null;
        try {
            Json jsonService = SystemController.getService(Json.class);
            String json = jsonService.toJson(request);

            KeyCipher cipher;
            try {
                CryptoService cryptoService = SystemController.getService(CryptoService.class);
                cipher = cryptoService.getKeyCipher(remoteKey);
            } catch (Exception ignored) {
                throw new RuntimeException("remoteKey error");
            }
            byte[] encrypt = cipher.encrypt(json.getBytes(StandardCharsets.UTF_8));

            connection = buildConnection(url);
            try (OutputStream outStream = connection.getOutputStream()) {
                outStream.write(encrypt);
                outStream.flush();
            }

            try (InputStream inputStream = connection.getInputStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(inputStream.available());
                FileUtil.copyStream(inputStream, bos);
                byte[] decryptedData = cipher.decrypt(bos.toByteArray());
                return jsonService.fromJson(new String(decryptedData, StandardCharsets.UTF_8), ResponseImpl.class);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException || e instanceof FileNotFoundException) {
                throw e;
            } else {
                throw new RuntimeException(String.format("Remote server [%s] request error: %s.",
                        url,
                        e.getMessage()));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection buildConnection(String url) throws Exception {
        HttpURLConnection conn;
        URL http = new URL(url);
        if (url.startsWith("https:")) {
            if (ssf == null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(new KeyManager[0], new TrustManager[]{TRUST_ALL_MANAGER}, new SecureRandom());
                ssf = sslContext.getSocketFactory();
            }
            HttpsURLConnection httpsConn = (HttpsURLConnection) http.openConnection();
            httpsConn.setSSLSocketFactory(ssf);
            httpsConn.setHostnameVerifier((hostname, session) -> true);
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) http.openConnection();
        }

        setConnectionProperties(conn);

        return conn;
    }

    private static void setConnectionProperties(HttpURLConnection conn) throws Exception {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(60000);
        conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");// 设置文件类型
        conn.setRequestProperty("accept", "*/*");// 设置接收类型否则返回415错误
        conn.setInstanceFollowRedirects(false);// 不处理重定向，否则“双因子密钥需要刷新”提示信息收不到。。。
    }
}
