package qingzhou.api.type;

import qingzhou.api.Item;

import java.util.Map;

/**
 * 表单数据联动
 */
public interface Echo {
    String ACTION_ECHO = "echo";

    void echoData(String echoGroup, Map<String, String> params, DataBuilder dataBuilder);

    interface DataBuilder {
        void addData(String field, String value);

        void addData(String field, String value, Item[] options);
    }
}
