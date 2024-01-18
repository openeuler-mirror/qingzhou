package qingzhou.app.master.service;

import qingzhou.app.master.DataStoreImpl;
import qingzhou.framework.console.ConsoleConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeDataStoreImpl extends DataStoreImpl {
    private final Map<String, String> localNode;

    public NodeDataStoreImpl(File dataXmlFile) {
        super(dataXmlFile);
        localNode = new HashMap<>();
        localNode.put("id", ConsoleConstants.LOCAL_NODE_NAME);
        localNode.put("ip", "0.0.0.0");
        localNode.put("port", "9060");
        localNode.put("running", "true");
    }

    @Override
    public int getTotalSize(String type) throws Exception {
        return super.getTotalSize(type) + 1;
    }

    @Override
    public List<String> getDataIdInPage(String type, int pageSize, int pageNum) throws Exception {
        int start = (pageNum - 1) * pageSize + 1;
        String nodeExpression = "//" + type + "[position() >= " + start + " and position() < " + (start + pageSize - 1) + "]/@id";
        List<String> attributeList = xmlUtil.getAttributeList(nodeExpression);

        return attributeList == null ? new ArrayList<>() : attributeList;
    }

    @Override
    public List<Map<String, String>> getDataFieldByIds(String type, String[] ids, String[] fields) throws Exception {
        List<Map<String, String>> dataFieldByIds = new ArrayList<>();
        dataFieldByIds.add(localNode);
        dataFieldByIds.addAll(super.getDataFieldByIds(type, ids, fields));

        return dataFieldByIds;
    }

    @Override
    public Map<String, String> getDataById(String type, String id) throws Exception {
        if (ConsoleConstants.LOCAL_NODE_NAME.equals(id)) {
            return localNode;
        }

        return super.getDataById(type, id);
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) throws Exception {
        if (!ConsoleConstants.LOCAL_NODE_NAME.equals(id)) {
            super.updateDataById(type, id, data);
        }
    }

    @Override
    public void deleteDataById(String type, String id) throws Exception {
        if (!ConsoleConstants.LOCAL_NODE_NAME.equals(id)) {
            super.deleteDataById(type, id);
        }
    }

    @Override
    public boolean exists(String type, String id) throws Exception {
        if (ConsoleConstants.LOCAL_NODE_NAME.equals(id)) {
            return true;
        }

        return super.exists(type, id);
    }
}
