package qingzhou.api.type;

import qingzhou.api.Item;

public interface Option {
    String ACTION_OPTION = "option";
    String FIELD_NAME_PARAMETER = "FIELD_NAME_PARAMETER";

    String[] staticOptionFields();

    String[] dynamicOptionFields();

    Item[] optionData(String fieldName);
}
