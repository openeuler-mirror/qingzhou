package qingzhou.api.type;

import qingzhou.api.QingzhouModel;

public interface Delete extends QingzhouModel {
    String ACTION_CODE_DELETE = "delete";

    void delete(String id) throws Exception;
}
