package qingzhou.app.master.service;

import qingzhou.app.master.ConsoleDataStore;
import qingzhou.framework.FrameworkContext;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeDataStore extends ConsoleDataStore {
    private final Map<String, String> localNode;

    public NodeDataStore(File serverXml) {
        super(serverXml);
        localNode = new HashMap<>();
        localNode.put("id", FrameworkContext.LOCAL_NODE_NAME);
        localNode.put("ip", "0.0.0.0");
        localNode.put("port", "9060");
        localNode.put("running", "true");
    }

    @Override
    public List<Map<String, String>> getAllData(String type) {
        List<Map<String, String>> allData = super.getAllData(type);
        allData.add(0, localNode);
        return allData;
    }
}
