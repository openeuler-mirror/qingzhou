package qingzhou.http.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import io.netty.handler.ssl.SslContext;
import qingzhou.crypto.Cipher;

public class SslContextBuilder {
    static SslContext buildSslContext(Map<String, String> config, Cipher cipher) {
        String keystorePath = config.get("ssl_keystore_path");
        if (keystorePath == null || keystorePath.trim().isEmpty()) {
            throw new IllegalArgumentException("ssl_keystore_path is required when ssl_enabled=true");
        }

        File keystoreFile = new File(keystorePath.trim());
        if (!keystoreFile.isFile()) {
            throw new IllegalArgumentException("ssl keystore file does not exist: " + keystoreFile
                    + ", generate it with bin/gen-keystore.sh");
        }

        String type = config.get("ssl_keystore_type");
        type = (type == null || type.trim().isEmpty()) ? "PKCS12" : type.trim().toUpperCase(Locale.ROOT);
        if (!"PKCS12".equals(type) && !"JKS".equals(type)) {
            throw new IllegalArgumentException("unsupported ssl_keystore_type: " + type + ", only PKCS12 or JKS is supported");
        }

        String password = config.get("ssl_keystore_password");
        if (password == null || password.isEmpty()) { // 口令强度策略交由部署方决定，此处只校验配置完整性
            throw new IllegalArgumentException("ssl_keystore_password is required when ssl_enabled=true"
                    + ", generate it with bin/gen-keystore.sh");
        }
        char[] keyPassword = cipher.tryDecrypt(password, "ssl_keystore_password").toCharArray();

        try (InputStream in = Files.newInputStream(keystoreFile.toPath())) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(in, keyPassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword);
            // 显式收敛协议：不指定时继承 JDK 默认，部分环境仍会启用 TLSv1.0/1.1
            return io.netty.handler.ssl.SslContextBuilder.forServer(keyManagerFactory).protocols(tlsProtocols()).build();
        } catch (Exception e) {
            throw new IllegalStateException("failed to load ssl keystore: " + keystoreFile, e);
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
    }

    // TLSv1.3 需要 JDK 11+，不可用时退到 TLSv1.2
    private static String[] tlsProtocols() {
        try {
            for (String protocol : SSLContext.getDefault().getSupportedSSLParameters().getProtocols()) {
                if ("TLSv1.3".equals(protocol)) return new String[]{"TLSv1.3", "TLSv1.2"};
            }
        } catch (NoSuchAlgorithmException e) {
            // 取不到支持列表时按最保守的 TLSv1.2 处理
        }
        return new String[]{"TLSv1.2"};
    }
}
