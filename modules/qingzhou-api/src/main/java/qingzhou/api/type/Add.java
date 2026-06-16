package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;

public interface Add extends QingzhouModel {
    String ACTION_CODE_CREATE = "create";
    String ACTION_CODE_ADD = "add";

    void add(Map<String, String> data) throws Exception;
}
