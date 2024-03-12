package qingzhou.api.type;

import qingzhou.api.Request;
import qingzhou.api.Response;

import java.util.Map;

/**
 * Createable 接口定义了对象的创建能力，扩展了 Deletable 和 Editable 接口。
 * 该接口主要提供了与创建操作相关的功能定义。
 */
public interface Createable extends Deletable, Editable {
    // 定义创建操作的常量名称
    String ACTION_NAME_CREATE = "create";

    // 定义添加操作的常量名称
    String ACTION_NAME_ADD = "add";

    default Map<String, String> add(Request req, Response resp) throws Exception {
        return null;
    }
}
