package qingzhou.api.type;

import java.util.Map;

/**
 * 定义了对象的创建能力。
 */
public interface Addable extends Deletable, Updatable {
    void addData(Map<String, String> data) throws Exception;
}
