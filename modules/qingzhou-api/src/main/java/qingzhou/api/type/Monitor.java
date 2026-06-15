package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;

public interface Monitor extends QingzhouModel {
    String ACTION_CODE_MONITOR = "monitor";

    Map<String, String> monitor(String id) throws Exception;
}
