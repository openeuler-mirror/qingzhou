package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;
import qingzhou.api.Request;

public interface Add extends QingzhouModel {
    String ACTION_CODE_CREATE = "create";
    String ACTION_CODE_ADD = "add";

    void add(Request request, Map<String, String> data) throws Exception;
}
