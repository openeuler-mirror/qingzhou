package qingzhou.remote.server;

import qingzhou.remote.Request;
import qingzhou.remote.Response;
import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Handler;

public class ServerHandler implements Handler {

    @Override
    public Object received(Object message, Channel channel) {
        if (message instanceof Request) {
            Request request = (Request) message;
            // todo 调用业务逻辑
            System.out.println("收到请求：" + request.getData());
            Object req = request.getData();
            int res = ((Integer) req) + 1;

            Response response = new Response();
            response.setId(request.getId());
            response.setData(res);
            return response;
        } else if (message instanceof Response) {
            return ((Response) message).getData();
        }
        return message;
    }
}
