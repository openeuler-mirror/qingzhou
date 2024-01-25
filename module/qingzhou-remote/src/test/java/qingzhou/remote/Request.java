package qingzhou.remote;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class Request implements Serializable {
    private static AtomicLong idCounter = new AtomicLong();

    private long id;
    private Object data;


    public Request() {
        this.id = idCounter.getAndIncrement();
    }

    public long getId() {
        return id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
