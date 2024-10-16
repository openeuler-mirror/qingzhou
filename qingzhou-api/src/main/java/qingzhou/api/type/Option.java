package qingzhou.api.type;

import qingzhou.api.Item;

public interface Option {
    String[] dynamicOptionFields();

    Item[] optionData(String fieldName);
}
