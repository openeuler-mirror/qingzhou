package qingzhou.crypto;

/**
 * 按时段派生密钥的报文加解密器，声明与 {@link Cipher} 一致，成对呈现。
 * <p>
 * 实例绑定一把根密钥与一个作用域，由 {@link Crypto#getEpochKeyRotatingCipher} 创建。
 * {@link #encrypt} 自动取当前时间戳并派生该时段密钥；{@link #decrypt} 按报文自带
 * 时间戳派生同一把钥，调用方无需感知时段与报文头。报文格式由 {@link ReplayGuardCipher} 封装。
 * <p>
 * 安全契约：根密钥带外预分发、永不上网；持有任一时段密钥推不出根密钥与其他时段密钥（HKDF 单向）。
 * <p>
 * 边界：根密钥静态，故不提供前向保密（根密钥一旦泄露即可派生全部时段）；轮换的目的是把单时段密钥
 * 泄露的爆炸半径压到该时段窗口（当前实现 10 分钟）。派生密钥为 32 字节 AES-256，Grover 下仍保 128 位，
 * 后量子强度来自密钥长度而非轮换本身。
 */
public interface EpochKeyRotatingCipher extends AadCipher {
}
