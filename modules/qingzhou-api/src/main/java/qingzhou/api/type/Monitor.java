package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;
import qingzhou.api.Request;

public interface Monitor extends QingzhouModel {
    String ACTION_CODE_MONITOR = "monitor";

    Map<String, String> monitor(Request request) throws Exception;

}
