package qingzhou.api.action;

import java.util.Map;

import qingzhou.api.QingzhouModel;

public interface Update extends QingzhouModel {
    String ACTION_CODE_UPDATE = "update";

    void update(String id, Map<String, String> data) throws Exception;
}
