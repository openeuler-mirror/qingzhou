package qingzhou.api.type;

import qingzhou.api.Request;

import java.util.Map;

public interface Validate {
    String ACTION_VALIDATE = "validate";
    String IS_ADD_OR_UPDATE_NON_MODEL_PARAMETER = "IS_ADD_OR_UPDATE_NON_MODEL_PARAMETER";

    Map<String, String> validate(Request request) throws Exception;
}
