package qingzhou.remote;

import java.io.Serializable;

public class Response implements Serializable {

    private long id;

    private Object data;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
