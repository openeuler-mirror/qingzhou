package qingzhou.api.type;

import qingzhou.api.QingzhouModel;

public interface SwitchSpace extends QingzhouModel {
    String ACTION_CODE_switchspace = "switchspace";
    String ACTION_CODE_currentspace = "currentspace";

    void switchSpace(String id) throws Exception;

    String currentSpace();
}
