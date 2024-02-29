package qingzhou.remote.net;

public interface Codec {

    byte[] encode(Object message, Channel channel);

    Object decode(byte[] message, Channel channel);
}
