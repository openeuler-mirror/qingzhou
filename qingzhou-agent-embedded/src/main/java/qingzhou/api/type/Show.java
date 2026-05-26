package qingzhou.api.type;

import java.util.Map;
import qingzhou.api.QingzhouModel;
import qingzhou.api.Request;

public interface Show extends QingzhouModel {
    String ACTION_CODE_SHOW = "show";

    Map<String, String> show(Request request) throws Exception;
}