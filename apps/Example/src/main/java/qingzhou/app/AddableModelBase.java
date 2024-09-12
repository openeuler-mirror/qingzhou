package qingzhou.app;

import qingzhou.api.ModelBase;
import qingzhou.api.type.Addable;

import java.util.List;
import java.util.Map;

public class AddableModelBase extends ModelBase implements Addable {

    private final MemoryDataStoreDemo testData = new MemoryDataStoreDemo(idFieldName());

    @Override
    public void addData(Map<String, String> data) {
        testData.addData(data.get(idFieldName()), data);
    }

    @Override
    public void deleteData(String id) {
        testData.deleteData(id);
    }

    @Override
    public String[] allIds() {
        return testData.allIds();
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        return testData.listData(pageNum, pageSize, fieldNames);
    }

    @Override
    public int totalSize() {
        return testData.totalSize();
    }

    @Override
    public Map<String, String> showData(String id) {
        return testData.showData(id);
    }

    @Override
    public void updateData(Map<String, String> data) {
        testData.updateData(data.get(idFieldName()), data);
    }
}
