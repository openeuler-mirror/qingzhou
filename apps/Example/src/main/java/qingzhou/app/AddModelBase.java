package qingzhou.app;

import qingzhou.api.ModelBase;
import qingzhou.api.type.General;

import java.util.List;
import java.util.Map;

public class AddModelBase extends ModelBase implements General {

    private final MemoryDataStoreDemo testData = new MemoryDataStoreDemo(idField());

    @Override
    public void addData(Map<String, String> data) {
        testData.addData(data);
        ExampleMain.logger.info("addData:" + data);
    }

    @Override
    public void deleteData(String id) {
        testData.deleteData(id);
        ExampleMain.logger.info("deleteData:" + id);
    }

    @Override
    public String[] allIds(Map<String, String> query) {
        return testData.allIds(query);
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) {
        return testData.listData(pageNum, pageSize, showFields, query);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return testData.totalSize(query);
    }

    @Override
    public Map<String, String> showData(String id) {
        return testData.showData(id);
    }

    @Override
    public void updateData(Map<String, String> data) {
        testData.updateData(getAppContext().getCurrentRequest().getId(), data);
        ExampleMain.logger.info("updateData:" + data);
    }

    @Override
    public int pageSize() {
        return 3;
    }
}
