package qingzhou.app.tomcat;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.*;
import qingzhou.xml.Doc;
import qingzhou.xml.Xml;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Model(code = "connector", order = 4,
        name = {"Connector通道", "en:Connector"},
        info = {"展示Tomcat Connector配置信息", "en:Display Tomcat Connector configuration"},
        icon = "Connection",
        menu = "container")
public class Connector extends qingzhou.api.ModelBase implements List, Show , Add, Update, Delete {
    private java.util.List<Map<String, String>> db;
    private static final String[] CONNECTOR_FIELDS = {"protocol", "port", "address", "maxThreads", "minSpareThreads", "acceptCount", "connectionTimeout", "redirectPort"};
    private static final Map<String, String> CONNECTOR_DEFAULTS = new HashMap<>();
    static {
        CONNECTOR_DEFAULTS.put("protocol", "HTTP/1.1");
        CONNECTOR_DEFAULTS.put("address", "0.0.0.0");
        CONNECTOR_DEFAULTS.put("maxThreads", "200");
        CONNECTOR_DEFAULTS.put("minSpareThreads", "10");
        CONNECTOR_DEFAULTS.put("acceptCount", "100");
        CONNECTOR_DEFAULTS.put("connectionTimeout", "20000");
        CONNECTOR_DEFAULTS.put("redirectPort", "8443");
    }
    public Connector() {}

    @ModelField(id = true,
            name = {"Connector ID", "en:Connector ID"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"协议", "en:Protocol"},
            info = {"Connector使用的协议", "en:Protocol used by Connector"},
            list = true, search = true,
            show = true)
    public String protocol;

    @ModelField(
            name = {"端口", "en:Port"},
            info = {"Connector监听的端口", "en:Port number for Connector"},
            list = true, required = true, search = true, port = true, update = false,
            show = true)
    public String port;

    @ModelField(
            name = {"地址", "en:Address"},
            info = {"Connector绑定的地址", "en:Address bound by Connector"},
            list = true, search = true, host = true,
            show = true)
    public String address;

    @ModelField(
            name = {"最大线程数", "en:Max Threads"},
            info = {"最大工作线程数", "en:Maximum number of worker threads"},
            list = true, numeric = true,
            show = true)
    public String maxThreads;

    @ModelField(
            name = {"最小空闲线程", "en:Min Spare Threads"},
            info = {"最小空闲线程数", "en:Minimum number of spare threads"},
            list = true, numeric = true,
            show = true)
    public String minSpareThreads;

    @ModelField(
            name = {"等待队列", "en:Accept Count"},
            info = {"等待队列长度", "en:Length of the wait queue"},
            list = true, numeric = true,
            show = true)
    public String acceptCount;

    @ModelField(
            name = {"超时时间", "en:Timeout"},
            info = {"连接超时时间(毫秒)", "en:Connection timeout in milliseconds"},
            show = true, numeric = true)
    public String connectionTimeout;

    @ModelField(
            name = {"重定向端口", "en:Redirect Port"},
            info = {"SSL重定向端口", "en:SSL redirect port"},
            show = true, port = true)
    public String redirectPort;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        db = parseServerXml();
        String searchPort = query != null ? query.get("port") : null;
        String searchProtocol = query != null ? query.get("protocol") : null;
        String searchAddress = query != null ? query.get("address") : null;

        for (Map<String, String> connector : db) {
            if (searchPort != null && !searchPort.isEmpty()) {
                String port = connector.get("port");
                if (port == null || !port.contains(searchPort)) {
                    continue;
                }
            }
            if (searchProtocol != null && !searchProtocol.isEmpty()) {
                String protocol = connector.get("protocol");
                if (protocol == null || !protocol.contains(searchProtocol)) {
                    continue;
                }
            }
            if (searchAddress != null && !searchAddress.isEmpty()) {
                String address = connector.get("address");
                if (address == null || !address.contains(searchAddress)) {
                    continue;
                }
            }
            result.add(new String[]{
                    connector.get("id"),
                    connector.get("protocol"),
                    connector.get("port"),
                    connector.get("address"),
                    connector.get("maxThreads"),
                    connector.get("minSpareThreads"),
                    connector.get("acceptCount")
            });
        }
        return result;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return db.size();

    }

    @Override
    public boolean contains(String id) {
        for (Map<String, String> connector : db) {
            if (connector.get("id").equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> show(Request request) {
        String id = request.getId();
        if (id == null) {
            return new HashMap<>();
        }
        for (Map<String, String> connector : db) {
            if (connector.get("id").equals(id)) {
                return connector;
            }
        }
        return new HashMap<>();
    }

    @Override
    public void add(Request request, Map<String, String> data) throws Exception {
        String confPath = getAppContext().getProperties().getProperty("conf.path");
        File file = new File(confPath);

        Xml xml = getAppContext().getService(Xml.class);
        Doc doc = xml.parse(file);

        String newPort = data.get("port");
        java.util.List<Properties> existingNodes = doc.getNodes("//Connector[@port='" + newPort + "']");
        if (!existingNodes.isEmpty()) {
            request.getResponse().success(false).msg("端口号[" + newPort + "]已存在，不能重复添加");
            return;
        }

        Properties attrs = new Properties();
        for (String field : CONNECTOR_FIELDS) {
            String value = data.get(field);
            if (value == null || value.isEmpty()) {
                value = CONNECTOR_DEFAULTS.get(field);
            }
            if (value != null) {
                attrs.setProperty(field, value);
            }
        }
        doc.addNode("//Service", "Connector", attrs);
        doc.write(file);
    }

    @Override
    public void delete(String id) throws Exception {
        String confPath = getAppContext().getProperties().getProperty("conf.path");
        File file = new File(confPath);

        Xml xml = getAppContext().getService(Xml.class);
        Doc doc = xml.parse(file);

        doc.deleteNode("//Connector[@port='" + id + "']");
        doc.write(file);
    }

    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        String id = request.getId();
        if (id == null) {
            throw new IllegalArgumentException("Connector ID is required for update");
        }

        String confPath = getAppContext().getProperties().getProperty("conf.path");
        File file = new File(confPath);

        Xml xml = getAppContext().getService(Xml.class);
        Doc doc = xml.parse(file);

        Properties attrs = new Properties();
        for (String field : CONNECTOR_FIELDS) {
            String value = data.get(field);
            attrs.setProperty(field, value != null ? value : "");
        }
        attrs.setProperty("port", id);

        doc.updateNode("//Connector[@port='" + id + "']", attrs);
        doc.write(file);
    }


    private java.util.List<Map<String, String>> parseServerXml() {
        java.util.List<Map<String, String>> result = new ArrayList<>();
        Xml xml = getAppContext().getService(Xml.class);
        try {
            String confPath = getAppContext().getProperties().getProperty("conf.path");
            if (confPath == null || confPath.isEmpty()) {
                System.err.println("conf.path is empty.");
                return result;
            }
            File file = new File(confPath);
            Doc doc = xml.parse(file);
            java.util.List<Properties> nodes = doc.getNodes("//Connector");
            for (Properties p: nodes) {
                Map<String, String> c1 = new HashMap<>();
                c1.put("id", p.getProperty("port"));
                c1.put("protocol", p.getProperty("protocol", "HTTP/1.1"));
                c1.put("port", p.getProperty("port"));
                c1.put("connectionTimeout", p.getProperty("connectionTimeout", "20000"));
                c1.put("redirectPort", p.getProperty("redirectPort", ""));
                c1.put("address", p.getProperty("address", "0.0.0.0"));
                c1.put("maxThreads", p.getProperty("maxThreads", "200"));
                c1.put("minSpareThreads", p.getProperty("minSpareThreads", "10"));
                c1.put("acceptCount", p.getProperty("acceptCount", "100"));
                result.add(c1);
            }
        } catch (Throwable e) {
            System.err.println("parse server xml error: " + e.getMessage());
        }
        return result;
    }
}
