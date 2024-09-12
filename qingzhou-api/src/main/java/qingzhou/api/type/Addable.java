package qingzhou.api.type;

import java.util.Map;

/**
 * 定义了对象的创建能力。
 */
public interface Addable extends Deletable, Updatable {
    String ACTION_CREATE = "create";
    String ACTION_ADD = "add";

    void addData(Map<String, String> data) throws Exception;
}
