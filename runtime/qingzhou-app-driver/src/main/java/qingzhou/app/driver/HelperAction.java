package qingzhou.app.driver;

import qingzhou.dto.RequestImpl;

public interface HelperAction {
    void execute(RequestImpl request) throws Exception;
}
