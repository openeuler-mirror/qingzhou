package qingzhou.remote.impl.net;

public interface Codec {

    byte[] encode(Object message, Channel channel);

    Object decode(byte[] message, Channel channel);
}
