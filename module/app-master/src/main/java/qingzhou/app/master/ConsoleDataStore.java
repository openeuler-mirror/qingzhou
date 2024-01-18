package qingzhou.app.master;

import qingzhou.framework.api.DataStore;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.XmlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleDataStore implements DataStore {
    private final XmlUtil xmlUtil;
    private final String rootPath;

    public ConsoleDataStore() {
        this.rootPath = "/root/console";
        File serverXml = FileUtil.newFile(Main.getFC().getDomain(), "conf", "server.xml");
        xmlUtil = new XmlUtil(serverXml);
    }

    @Override
    public List<Map<String, String>> getAllData(String type) {
        List<Map<String, String>> datas = xmlUtil.getAttributesList("//" + type);
        if (datas == null) {
            return new ArrayList<>();
        }
        return datas;
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) {
        String tags = "//" + type + "s";
        if (StringUtil.notBlank(id)) {
            properties.put(ListModel.FIELD_NAME_ID, id);
        }
        if (!xmlUtil.isNodeExists(tags)) {
            xmlUtil.addNew(this.rootPath, type + "s", null);
        }
        xmlUtil.addNew(tags, type, properties);
        xmlUtil.write();
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) {
        deleteDataById(type, id);
        addData(type, id, data);
    }

    @Override
    public void deleteDataById(String type, String id) {
        xmlUtil.delete("//" + type + "s/" + type + "[@id='" + id + "']");
        xmlUtil.write();
    }
}