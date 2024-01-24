package qingzhou.remote.client;

import qingzhou.remote.Request;
import qingzhou.remote.Response;
import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Codec;
import qingzhou.serializer.Serializer;

public class ClientProtocolCodec implements Codec {

    private Serializer serializer;

    public ClientProtocolCodec(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public byte[] encode(Object message, Channel channel) {
        try {
            if (message instanceof Request) {
                return serializer.serialize(message);
            } else if (message instanceof byte[]) {
                return (byte[]) message;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object decode(byte[] message, Channel channel) {
        try {
            return serializer.deserialize(message, Response.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }
}
