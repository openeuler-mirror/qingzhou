package qingzhou.api.action;

public interface Namespace {
    void namespace(String id) throws Exception;

    String current();
}