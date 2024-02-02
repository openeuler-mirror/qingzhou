package qingzhou.console.remote;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyCipher;
import qingzhou.framework.ResponseImpl;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.serializer.Serializer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;

public class RemoteClient {
    private static SSLSocketFactory ssf;
    public static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManagerInternal();

    public static ResponseImpl sendReq(String url, Object object, String remoteKey) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = buildConnection(url);
            setConnectionProperties(connection);

            KeyCipher cipher;
            try {
                CryptoService cryptoService = ConsoleWarHelper.getCryptoService();
                String localKey = ConsoleWarHelper.getConfigManager().getKey(ConsoleConstants.localKeyName);
                cipher = cryptoService.getKeyCipher(cryptoService.getKeyCipher(localKey).decrypt(remoteKey));
            } catch (Exception ignored) {
                throw new RuntimeException("remoteKey error");
            }

            Serializer serializer = ConsoleWarHelper.getSerializer();
            byte[] serialize = serializer.serialize(object);
            byte[] encrypt = cipher.encrypt(serialize);
            OutputStream outStream = connection.getOutputStream();
            outStream.write(encrypt);
            outStream.flush();
            outStream.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                String location = connection.getHeaderField("Location");
                if (StringUtil.isBlank(location)) {
                    URL tempUrl = connection.getURL();
                    throw ExceptionUtil.unexpectedException(String.format("Remote server [%s:%s] request error: %s. Please check the logs for details", tempUrl.getHost(), tempUrl.getPort(), "The redirect address is wrong."));
                }
                return sendReq(location, object, remoteKey);
            } else {
                try (InputStream inputStream = connection.getInputStream()) {
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                    int length = objectInputStream.readInt();
                    byte[] deserializeBytes = new byte[length];
                    int read = 0;
                    while (length > read) {
                        int r = objectInputStream.read(deserializeBytes, read, length - read);
                        read += r;
                    }
                    // 再读取一次返回 -1，表示当前块已结束，close时才能将 sun.net.www.protocol.https.HttpsClient放入缓存中，实现复用
                    int last = objectInputStream.read();
                    if (last != -1) {
                        ConsoleWarHelper.getLogger().warn("The data parsing is abnormal...");
                    }
                    if (read == deserializeBytes.length) {
                        byte[] decrypt = cipher.decrypt(deserializeBytes);
                        return serializer.deserialize(decrypt, ResponseImpl.class);
                    } else {
                        throw ExceptionUtil.unexpectedException("The expected file size was not reached");
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof SSLException && e.getMessage() != null && e.getMessage().contains("Unrecognized SSL message")) {
                url = "http" + url.substring(5);
                return sendReq(url, object, remoteKey);
            }
            if (e instanceof RuntimeException || e instanceof FileNotFoundException) {
                throw e;
            } else {
                URL tempUrl = new URL(url);
                throw ExceptionUtil.unexpectedException(String.format("Remote server [%s:%s] request error: %s. Please check the logs for details", tempUrl.getHost(), tempUrl.getPort(), e.getMessage()));
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
