package qingzhou.app.master;

import qingzhou.framework.api.DataStore;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.XmlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataStoreImpl implements DataStore {
    protected XmlUtil xmlUtil;
    private final String rootPath;

    public DataStoreImpl(File dataXmlFile) {
        this.rootPath = "/root/console";
        xmlUtil = new XmlUtil(dataXmlFile);
    }

    @Override
    public List<Map<String, String>> getAllData(String type) throws Exception {
        List<Map<String, String>> datas = xmlUtil.getAttributesList("//" + type);
        if(datas == null){
            return new ArrayList<>();
        }
        return datas;
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) throws Exception {
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
    public List<String> getDataIdInPage(String type, int pageSize, int pageNum) throws Exception {
        int start = (pageNum - 1) * pageSize + 1;
        String nodeExpression = "//" + type + "[position() >= " + start + " and position() < " + (start + pageSize) + "]/@id";
        return xmlUtil.getAttributeList(nodeExpression);
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) throws Exception {
        deleteDataById(type, id);
        addData(type, id, data);
    }

    @Override
    public void deleteDataById(String type, String id) throws Exception {
        xmlUtil.delete("//" + type + "s/" + type + "[@id='" + id + "']");
        xmlUtil.write();
    }
}