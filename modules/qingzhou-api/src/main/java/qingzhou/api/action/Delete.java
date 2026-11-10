package qingzhou.api.action;

public interface Delete {
    String ACTION_CODE_DELETE = "delete";

    void delete(String id) throws Exception;
}
