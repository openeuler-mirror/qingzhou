package qingzhou.api.action;

import qingzhou.api.QingzhouModel;

public interface Namespace extends QingzhouModel {
    String ACTION_CODE_NAMESPACE = "namespace";
    String ACTION_CODE_CURRENT = "current";

    void namespace(String id) throws Exception;

    String current();
}