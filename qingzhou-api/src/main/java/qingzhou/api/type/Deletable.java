package qingzhou.api.type;

/**
 * 定义了删除操作的相关行为。
 */
public interface Deletable extends Listable {
    void deleteData(String id) throws Exception;
}
