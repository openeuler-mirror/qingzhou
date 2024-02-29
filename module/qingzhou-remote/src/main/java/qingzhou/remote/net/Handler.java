package qingzhou.remote.net;

public interface Handler {

    default Object sent(Object message, Channel channel) {
        return message;
    }


    default Object received(Object message, Channel channel) {
        return message;
    }

}
