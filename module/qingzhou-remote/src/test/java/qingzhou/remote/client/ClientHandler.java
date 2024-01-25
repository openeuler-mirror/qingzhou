package qingzhou.remote.client;

import qingzhou.remote.Request;
import qingzhou.remote.Response;
import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Handler;

public class ClientHandler implements Handler {

    @Override
    public Object sent(Object message, Channel channel) {
        Request request = new Request();
        request.setData(message);
        // todo
        return request;
    }

    @Override
    public Object received(Object message, Channel channel) {
        if (message instanceof Response) {
            return ((Response) message).getData();
        }
        return message;
    }
}
