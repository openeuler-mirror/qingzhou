package qingzhou.api.type;

/**
 * Combined接口定义了组合数据展示，如有show有list等。
 */
public interface Combined {
    String ACTION_COMBINED = "combined";

    void combinedData(String id, DataBuilder dataBuilder) throws Exception;

    interface DataBuilder {
        <T> T buildData(Class<? extends CombinedData> dataType);

        void addData(CombinedData data);
    }

    interface CombinedData {
        CombinedData header(String header);

        CombinedData model(String model);

        CombinedData type(String type);
    }

    interface ShowData extends CombinedData {
        void addData(String fieldName, String fieldValue);
    }

    interface UmlData extends CombinedData {
        void setData(String umlData);
    }

    interface ListData extends CombinedData {
        void setFields(String[] fields);

        void addFieldValues(String[] fieldValues);
    }
}
