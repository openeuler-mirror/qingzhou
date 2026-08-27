package qingzhou.api.action;

import java.util.Map;

import qingzhou.api.QingzhouModel;

public interface Show extends QingzhouModel {
    String ACTION_CODE_SHOW = "show";

    Map<String, String> show(String id) throws Exception;
}
