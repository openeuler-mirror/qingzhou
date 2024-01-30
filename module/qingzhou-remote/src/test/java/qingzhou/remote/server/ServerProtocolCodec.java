package qingzhou.remote.server;

import qingzhou.remote.Request;
import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Codec;
import qingzhou.serializer.Serializer;

public class ServerProtocolCodec implements Codec {
    private Serializer serializer;

    public ServerProtocolCodec(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public byte[] encode(Object message, Channel channel) {
        try {
            return serializer.serialize(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object decode(byte[] message, Channel channel) {
        try {
            return serializer.deserialize(message, Request.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }
}
