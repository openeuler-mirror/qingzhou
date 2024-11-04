package qingzhou.api.type;

/**
 * Combined接口定义了组合数据展示，如有show有list等。
 */
public interface Combined {
    String ACTION_COMBINED = "combined";

    void combinedData(String id, DataBuilder dataBuilder) throws Exception;

    interface DataBuilder {
        ShowData buildShowData();

        UmlData buildUmlData();

        ListData buildListData();

        void add(CombinedData data);
    }

    interface CombinedData {
        CombinedData header(String header);

        CombinedData model(String model);
    }

    interface ShowData extends CombinedData {
        void putData(String fieldName, String fieldValue);
    }

    interface UmlData extends CombinedData {
        void setUmlData(String umlData);
    }

    interface ListData extends CombinedData {
        void setFieldNames(String[] fieldNames);

        void addFieldValues(String[] fieldValues);
    }
}
