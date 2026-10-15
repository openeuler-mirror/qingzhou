package qingzhou.http.server.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import io.netty.handler.ssl.SslContext;
import qingzhou.crypto.Cipher;
import qingzhou.common.Configs;
import qingzhou.logger.Logger;

final class SslContextBuilder { // 仅本包使用，不对外暴露
    // 不指定时继承 JDK 默认，部分环境仍会启用 TLSv1.0/1.1 与 3DES 等弱套件，故显式收敛
    private static final String[] CIPHERS = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
    };

    private SslContextBuilder() {
    }

    static SslContext buildSslContext(Map<String, String> config, Cipher cipher, Logger logger) {
        Configs configs = new Configs(config);
        String keystorePath = configs.notBlank("ssl_keystore_path");

        File keystoreFile = new File(keystorePath.trim());
        if (!keystoreFile.isFile()) {
            throw new IllegalArgumentException("ssl keystore file does not exist: " + keystoreFile
                    + ", generate it with bin/qingzhou.sh keystore");
        }

        String type = config.get("ssl_keystore_type");
        type = (type == null || type.trim().isEmpty()) ? "PKCS12" : type.trim().toUpperCase(Locale.ROOT);
        if (!"PKCS12".equals(type) && !"JKS".equals(type)) {
            throw new IllegalArgumentException("unsupported ssl_keystore_type: " + type + ", only PKCS12 or JKS is supported");
        }

        String password = configs.notBlank("ssl_keystore_password");
        char[] keyPassword = cipher.tryDecrypt(password.trim(), "qingzhou-http-server.ssl_keystore_password").toCharArray();

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
                // 回退时仍须排除弱套件：CBC/3DES/RC4/匿名/导出级一律剔除；剩余为空则交由握手失败，宁可启动不可用也不静默降级
                logger.warn("no strong cipher suite is supported by this JDK, falling back to filtered JDK defaults");
                List<String> filtered = new ArrayList<>();
                for (String suite : supported) {
                    String upper = suite.toUpperCase(Locale.ROOT);
                    if (upper.contains("CBC") || upper.contains("3DES") || upper.contains("RC4")
                            || upper.contains("NULL") || upper.contains("EXPORT") || upper.contains("ANON")) continue;
                    filtered.add(suite);
                }
                return filtered;
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
