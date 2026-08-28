package qingzhou.api.action;

import qingzhou.api.QingzhouModel;

public interface SwitchSpace extends QingzhouModel {
    String ACTION_CODE_SWITCHSPACE = "switchspace";
    String ACTION_CODE_CURRENTSPACE = "currentspace";

    void switchSpace(String id) throws Exception;

    String currentSpace();
}
