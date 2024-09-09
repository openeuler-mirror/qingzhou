package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Addable;
import qingzhou.app.DataStore;
import qingzhou.app.ExampleMain;
import qingzhou.app.MemoryDataStore;

import java.util.List;
import java.util.Map;

@Model(code = "test", icon = "stack",
        menu = ExampleMain.MAIN_MENU, order = 1,
        name = {"测试模块", "en:TestListModel"},
        info = {"测试模块的说明信息。",
                "en:Description of the test module."})
public class TestModel extends ModelBase implements Addable {
    public static final DataStore dataStore = new MemoryDataStore();

    @ModelField(
            required = true,
            list = true,
            name = {"名称", "en:Name"},
            info = {"名称信息。",
                    "en:Name information."})
    public String name;

    @ModelField(
            type = FieldType.number,
            required = true,
            min = 1, max = 100,
            list = true,
            name = {"名称", "en:Name"},
            info = {"名称信息。",
                    "en:Name information."})
    public int num;

    @ModelField(
            type = FieldType.bool,
            required = true,
            list = true,
            name = {"开关", "en:Switch"},
            info = {"开关说明信息。",
                    "en:Switch description information."})
    public Boolean bool = false;

    @Override
    public void addData(Map<String, String> data) {
        dataStore.addData(data.get(idFieldName()), data);
    }

    @Override
    public void deleteData(String id) {
        dataStore.deleteData(id);
    }

    @Override
    public String idFieldName() {
        return "name";
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        return dataStore.listData(pageNum, pageSize, fieldNames);
    }

    @Override
    public int totalSize() {
        return dataStore.totalSize();
    }

    @Override
    public Map<String, String> showData(String id) {
        return dataStore.showData(id);
    }

    @Override
    public void updateData(Map<String, String> data) {
        dataStore.updateData(data.get(idFieldName()), data);
    }
}
