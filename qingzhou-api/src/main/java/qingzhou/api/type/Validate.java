package qingzhou.api.type;

import qingzhou.api.Request;

import java.util.Map;

public interface Validate {
    String ACTION_VALIDATE = "validate";

    Map<String, String> validate(Request request, ValidationContext context) throws Exception;

    interface ValidationContext {
        boolean isAdd();

        boolean isUpdate();
    }
}
