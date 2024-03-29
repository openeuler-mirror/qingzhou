package qingzhou.remote;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

class HttpClient {
    private static final String CONTENT_LENGTH = "Content-Length";

    static void seqHttp(String url, Map<String, String> params) throws Exception {
        HttpURLConnection conn = buildConnection(url);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("accept", "*/*");
        conn.setInstanceFollowRedirects(false);

        String bodyStr = encodingParams(params);
        if (bodyStr != null) {
            byte[] b = bodyStr.getBytes();
            conn.setRequestProperty(CONTENT_LENGTH, String.valueOf(b.length));
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(b, 0, b.length);
            outputStream.flush();
            outputStream.close();
        }

        conn.connect();
        int responseCode = conn.getResponseCode();
        InputStream errorStream = conn.getErrorStream();
        InputStream responseStream = errorStream == null ? conn.getInputStream() : errorStream;
        String result = toString(responseStream);
        if (responseCode != 200) {
            System.out.printf("send request fail, url:%s, message:%s.%n", url, result);
        }
    }

    private static String toString(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        int len;
        byte[] bytes = new byte[1024 * 8];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while ((len = input.read(bytes)) != -1) {
            os.write(bytes, 0, len);
        }
        return os.toString("utf-8");
    }

    private static String encodingParams(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (null == params || params.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            sb.append(entry.getKey()).append('=');
            sb.append(URLEncoder.encode(value, "utf-8"));
            sb.append('&');
        }

        return sb.toString();
    }

    private static HttpURLConnection buildConnection(String url) throws NoSuchAlgorithmException, IOException, KeyManagementException {
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

    private static SSLSocketFactory ssf;
    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
