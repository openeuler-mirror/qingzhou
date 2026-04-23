package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;
import qingzhou.api.Request;

public interface Update extends QingzhouModel {
    String ACTION_CODE_UPDATE = "update";

    void update(Request request, Map<String, String> data) throws Exception;
}
