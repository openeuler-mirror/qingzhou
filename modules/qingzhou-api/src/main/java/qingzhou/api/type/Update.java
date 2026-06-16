package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;

public interface Update extends QingzhouModel {
    String ACTION_CODE_EDIT = "edit";
    String ACTION_CODE_UPDATE = "update";

    void update(String id, Map<String, String> data) throws Exception;
}
