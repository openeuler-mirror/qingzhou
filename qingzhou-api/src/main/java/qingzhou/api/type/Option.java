package qingzhou.api.type;

import qingzhou.api.Item;

public interface Option {
    String ACTION_CONTAINS = "contains";

    Item[] optionData(String fieldName);
}
