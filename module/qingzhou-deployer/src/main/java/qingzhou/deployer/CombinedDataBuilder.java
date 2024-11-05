package qingzhou.deployer;

import qingzhou.api.type.Combined;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CombinedDataBuilder implements Combined.DataBuilder, Serializable {
    private final List<Combined.CombinedData> dataList = new ArrayList<>();

    @Override
    public Combined.ShowData buildShowData() {
        return new ShowDataImpl();
    }

    @Override
    public Combined.UmlData buildUmlData() {
        return new UmlDataImpl();
    }

    @Override
    public Combined.ListData buildListData() {
        return new ListDataImpl();
    }

    @Override
    public void add(Combined.CombinedData data) {
        this.dataList.add(data);
    }

    public List<Combined.CombinedData> getDataList() {
        return dataList;
    }

    public static abstract class CombinedDataImpl implements Combined.CombinedData, Serializable {
        private String header;
        private String model;

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


        public String getHeader() {
            return header;
        }

        public String getModel() {
            return model;
        }
    }

    public static class ShowDataImpl extends CombinedDataImpl implements Combined.ShowData {
        private final Map<String, String> showData = new HashMap<>();

        @Override
        public void putData(String fieldName, String fieldValue) {
            this.showData.put(fieldName, fieldValue);
        }

        public Map<String, String> getShowData() {
            return showData;
        }
    }

    public static class UmlDataImpl extends CombinedDataImpl implements Combined.UmlData {
        private String umlData;

        @Override
        public void setUmlData(String umlData) {
            this.umlData = umlData;
        }

        public String getUmlData() {
            return umlData;
        }
    }

    public static class ListDataImpl extends CombinedDataImpl implements Combined.ListData {
        private String[] fieldNames;
        private final List<String[]> fieldValues = new ArrayList<>();

        @Override
        public void setFieldNames(String[] fieldNames) {
            this.fieldNames = fieldNames;
        }

        @Override
        public void addFieldValues(String[] fieldValues) {
            this.fieldValues.add(fieldValues);
        }

        public String[] getFieldNames() {
            return fieldNames;
        }

        public List<String[]> getFieldValues() {
            return fieldValues;
        }
    }
}
