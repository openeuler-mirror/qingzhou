package qingzhou.app.driver;

import qingzhou.dto.RequestImpl;

public interface SystemCall {
    void call(RequestImpl request) throws Exception;
}
