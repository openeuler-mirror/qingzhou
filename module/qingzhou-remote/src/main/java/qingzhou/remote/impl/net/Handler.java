package qingzhou.remote.impl.net;

public interface Handler {

    default Object sent(Object message, Channel channel) {
        return message;
    }


    default Object received(Object message, Channel channel) {
        return message;
    }

}
