package qingzhou.http.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import io.netty.handler.ssl.SslContext;
import qingzhou.crypto.Cipher;
import qingzhou.logger.Logger;

final class SslContextBuilder { // 仅本包使用，不对外暴露
    // 不指定时继承 JDK 默认，部分环境仍会启用 TLSv1.0/1.1 与 3DES 等弱套件，故显式收敛
    private static final String[] CIPHERS = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256"
    };

    private SslContextBuilder() {
    }

    static SslContext buildSslContext(Map<String, String> config, Cipher cipher, Logger logger) {
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
        if (password == null || password.trim().isEmpty()) { // 口令强度策略交由部署方决定，此处只校验配置完整性
            throw new IllegalArgumentException("ssl_keystore_password is required when ssl_enabled=true"
                    + ", generate it with bin/gen-keystore.sh");
        }
        char[] keyPassword = cipher.tryDecrypt(password, "qingzhou-http-server.ssl_keystore_password").toCharArray();

        try (InputStream in = Files.newInputStream(keystoreFile.toPath())) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(in, keyPassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword);
            // 显式收敛协议与套件：不指定时继承 JDK 默认，部分环境仍会启用 TLSv1.0/1.1 与弱套件
            return io.netty.handler.ssl.SslContextBuilder.forServer(keyManagerFactory)
                    .protocols(tlsProtocols())
                    .ciphers(supportedCiphers(logger))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("failed to load ssl keystore: " + keystoreFile, e);
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
    }

    // 逐个比对 JDK 支持清单：EC 证书下 RSA 套件会被全过滤，空套件会导致握手失败，故全不匹配时回退默认并告警
    private static List<String> supportedCiphers(Logger logger) {
        try {
            List<String> supported = Arrays.asList(SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites());
            List<String> enabled = new ArrayList<>();
            for (String cipher : CIPHERS) {
                if (supported.contains(cipher)) enabled.add(cipher);
            }
            if (enabled.isEmpty()) {
                logger.warn("no strong cipher suite is supported by this JDK, falling back to JDK defaults");
                return supported;
            }
            return enabled;
        } catch (NoSuchAlgorithmException e) {
            return Arrays.asList(CIPHERS); // 取不到支持列表时按声明值交给 Netty 过滤
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
