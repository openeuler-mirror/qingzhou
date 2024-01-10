package qingzhou.console.remote;

import qingzhou.framework.console.ConsoleConstants;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.servlet.UploadFileContext;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.console.ResponseImpl;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.ObjectUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.serializer.Serializer;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.List;

public class RemoteClient {
    private static SSLSocketFactory ssf;
    public static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManagerInternal();
    public static String NEW_LINE = "\r\n";
    private static final String PREFIX = "--";
    private static final String BOUNDART = "*****";

    public static ResponseImpl sendReq(String url, Object object, String remoteKey) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = buildConnection(url);
            setConnectionProperties(connection, true);

            PasswordCipher cipher;
            try {
                CryptoService cryptoService = ConsoleWarHelper.getCryptoService();
                qingzhou.crypto.KeyManager keyManager = cryptoService.getKeyManager();
                String localKey = keyManager.getKeyOrElseInit(ConsoleUtil.getSecureFile(ConsoleWarHelper.getDomain()), ConsoleConstants.localKeyName, null);
                cipher = cryptoService.getPasswordCipher(cryptoService.getPasswordCipher(localKey).decrypt(remoteKey));
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
                    objectInputStream.read();
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

    private static void setConnectionProperties(HttpURLConnection conn, boolean keepAlive) throws Exception {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(60000);
        conn.setRequestProperty("Connection", keepAlive ? "keep-alive" : "close");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");// 设置文件类型
        conn.setRequestProperty("accept", "*/*");// 设置接收类型否则返回415错误
        conn.setInstanceFollowRedirects(false);// 不处理重定向，否则“双因子密钥需要刷新”提示信息收不到。。。
    }

    public static String uploadFile(String url, String fileName) throws Exception {
        File file = FileUtil.newFile(fileName);
        HttpURLConnection conn = buildConnection(url);
        try {
            setConnectionProperties(conn, true);
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDART);
            conn.setChunkedStreamingMode(4 * 1024);

            try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                 OutputStream outputStream = conn.getOutputStream()) {
                DataOutputStream os = new DataOutputStream(outputStream);
                os.writeBytes(PREFIX + BOUNDART + NEW_LINE);
                os.writeBytes(("Content-Disposition: form-data; name=\"" + UploadFileContext.uploadFileParam + "\"; filename=\"" + file.getName() + "\"" + NEW_LINE));
                os.writeBytes(NEW_LINE);
                channel.transferTo(0, channel.size(), Channels.newChannel(os));
                os.writeBytes(NEW_LINE);
                os.writeBytes(PREFIX + BOUNDART + PREFIX + NEW_LINE);
                os.close();
            }
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream()) {
                    return ObjectUtil.inputStreamToString(is, StandardCharsets.UTF_8);
                }
            } else {
                throw new RuntimeException(String.format("Failed to upload file, url: %s, file: %s.", url, fileName));
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static void deleteFiles(String url, List<String> uploadFiles) {
        HttpURLConnection conn = null;
        try {
            conn = buildConnection(url);
            setConnectionProperties(conn, true);
            StringBuilder param = new StringBuilder();
            for (String uploadFile : uploadFiles) {
                param.append("deleteFiles=").append(URLEncoder.encode(uploadFile, "utf-8")).append("&");
            }
            try (OutputStream os = conn.getOutputStream()) {
                os.write(param.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
