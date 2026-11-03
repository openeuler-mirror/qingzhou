package qingzhou.command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import qingzhou.config.impl.Config;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.impl.CryptoImpl;

public abstract class Processor {
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    private File libDir;

    protected File getLibDir() {
        return libDir;
    }

    public void setLibDir(File libDir) {
        this.libDir = libDir;
    }

    public File getHomeDir() {
        return libDir.getParentFile().getParentFile();
    }

    // 此命令行处理器的名字
    public abstract String name();

    public abstract String info();

    public String[] supportedArgs() {
        return null;
    }

    public abstract void doCommandLine(String[] args) throws Exception;

    public void log(String msg) {
        System.out.println(msg);
    }

    protected File initInstance(String[] args) {
        String instanceName = args.length > 0 && !args[0].isEmpty() ? args[0] : "default";
        if (!isSafeInstanceName(instanceName)) {
            log("invalid instance name: " + instanceName);
            return null;
        }
        File instance = Paths.get(getHomeDir().getAbsolutePath(), "instances", instanceName).toFile();
        File configFile = Paths.get(instance.getAbsolutePath(), "conf", "qingzhou.properties").toFile();
        if (configFile.isFile()) {
            return instance;
        } else {
            log("instance does not exist: " + instanceName);
            return null;
        }
    }

    /**
     * 全局密钥属性名：与运行时同源读取实例配置（qingzhou.properties 的
     * qingzhou-crypto.global_cipher_key_name，缺省 global），供 init 写入与 encrypt 加密共用，
     * 避免自定义密钥名后写入位置与运行时读取位置错位。
     */
    protected static String globalCipherKeyName(File instanceDir) throws IOException {
        File props = new File(instanceDir, "conf/qingzhou.properties");
        // 读取失败直接抛出：用错密钥加密比命令失败更危险，不做静默降级
        Map<String, String> config = Config.parse(new String(Files.readAllBytes(props.toPath()), StandardCharsets.UTF_8));
        String name = config.get("qingzhou-crypto.global_cipher_key_name");
        return name == null || name.isEmpty() ? "global" : name; // 缺省值与 EnvCheck 一致
    }

    /**
     * 构建绑定到实例的加解密服务：实例路径与全局密钥属性名均与运行时同源，
     * 供 init（写密钥）与 encrypt（加密口令）共用，避免两侧各自拼装配置出现偏差。
     */
    protected static Crypto createCrypto(File instanceDir, String globalKeyName) {
        Map<String, String> config = new HashMap<>();
        config.put("qingzhou.instance", instanceDir.getAbsolutePath());
        config.put("global_cipher_key_name", globalKeyName);
        CryptoImpl crypto = new CryptoImpl();
        crypto.init(config);
        return crypto;
    }

    // 实例名直接拼进路径，须拒绝任何能逃出 instances 目录的字符，否则可读写其它实例配置/密钥
    private static boolean isSafeInstanceName(String name) {
        if (name == null || name.isEmpty()) return false;
        return !name.equals(".") && !name.contains("..") && !name.contains("/") && !name.contains("\\") && name.indexOf('\0') < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Processor)) return false;
        Processor user = (Processor) o;
        return name().equals(user.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
