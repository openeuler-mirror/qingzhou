package qingzhou.crypto;

/**
 * 按时段派生密钥的报文加解密器，声明与 {@link Cipher} 一致，成对呈现。
 * <p>
 * 实例绑定一把根密钥与一个作用域，由 {@link Crypto#getEpochPacketCipher} 创建。
 * {@link #encrypt} 自动取当前时间戳并派生该时段密钥；{@link #decrypt} 按报文自带
 * 时间戳派生同一把钥，调用方无需感知时段与报文头。报文格式由 {@link ReplayGuardCipher} 封装。
 * <p>
 * 安全契约：根密钥带外预分发、永不上网；持有任一时段密钥推不出根密钥与其他时段密钥（HKDF 单向）。
 */
public interface EpochPacketCipher extends AadCipher {
}
