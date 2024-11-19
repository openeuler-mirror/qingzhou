package qingzhou.core;

import qingzhou.api.type.Combined;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class CombinedDataBuilder implements Combined.DataBuilder, Serializable {
    public final java.util.List<Combined.CombinedData> data = new ArrayList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

    @Override
    public <T> T buildData(Class<? extends Combined.CombinedData> dataType) {
        if (dataType == Combined.ShowData.class) return (T) new Show();
        if (dataType == Combined.UmlData.class) return (T) new Uml();
        if (dataType == Combined.ListData.class) return (T) new List();
        throw new IllegalArgumentException();
    }

    @Override
    public void addData(Combined.CombinedData data) {
        this.data.add(data);
    }

    public static abstract class CombinedDataImpl implements Combined.CombinedData, Serializable {
        public String header; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String model; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String type = this.getClass().getSimpleName(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public Combined.CombinedData header(String header) {
            this.header = header;
            return this;
        }

        @Override
        public Combined.CombinedData model(String model) {
            this.model = model;
            return this;
        }
    }

    public static class Show extends CombinedDataImpl implements Combined.ShowData {
        public Map<String, String> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public void addData(String fieldName, String fieldValue) {
            this.data.put(fieldName, fieldValue);
        }
    }

    public static class Uml extends CombinedDataImpl implements Combined.UmlData {
        public String data; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public void setData(String umlData) {
            this.data = umlData;
        }
    }

    public static class List extends CombinedDataImpl implements Combined.ListData {
        public String[] fields; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public java.util.List<String[]> values = new ArrayList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public void setFields(String[] fields) {
            this.fields = fields;
        }

        @Override
        public void addFieldValues(String[] fieldValues) {
            this.values.add(fieldValues);
        }
    }
}
