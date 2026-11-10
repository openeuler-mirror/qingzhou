package qingzhou.api.action;

public interface Namespace {
    String ACTION_CODE_NAMESPACE = "namespace";
    String ACTION_CODE_CURRENT = "current";

    void namespace(String id) throws Exception;

    String current();
}