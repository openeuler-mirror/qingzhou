package qingzhou.app.tomcat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.*;

@Model(code = "connector", order = 2,
        name = {"Connector通道", "en:Connector"},
        info = {"展示Tomcat Connector配置信息", "en:Display Tomcat Connector configuration"},
        icon = "Fries")
public class Connector extends TomcatModelBase implements qingzhou.api.type.List, Show, Add, Update, Delete {
    private java.util.List<Map<String, String>> db;
    
    // Connector 字段定义
    private static final String[] CONNECTOR_FIELDS = {"protocol", "port", "address", "maxThreads", "minSpareThreads", "acceptCount", "connectionTimeout", "redirectPort"};
    
    // 默认值常量
    private static final String DEFAULT_PROTOCOL = "HTTP/1.1";
    private static final String DEFAULT_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_MAX_THREADS = 200;
    private static final int DEFAULT_MIN_SPARE_THREADS = 10;
    private static final int DEFAULT_ACCEPT_COUNT = 100;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 20000;
    private static final int DEFAULT_REDIRECT_PORT = 8443;
    
    private static final Map<String, String> CONNECTOR_DEFAULTS = new HashMap<>();

    static {
        CONNECTOR_DEFAULTS.put("protocol", DEFAULT_PROTOCOL);
        CONNECTOR_DEFAULTS.put("address", DEFAULT_ADDRESS);
        CONNECTOR_DEFAULTS.put("maxThreads", String.valueOf(DEFAULT_MAX_THREADS));
        CONNECTOR_DEFAULTS.put("minSpareThreads", String.valueOf(DEFAULT_MIN_SPARE_THREADS));
        CONNECTOR_DEFAULTS.put("acceptCount", String.valueOf(DEFAULT_ACCEPT_COUNT));
        CONNECTOR_DEFAULTS.put("connectionTimeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        CONNECTOR_DEFAULTS.put("redirectPort", String.valueOf(DEFAULT_REDIRECT_PORT));
    }

    public Connector() {
    }

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
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        db = parseServerXml();
        List<Map<String, String>> filtered = filterByQuery(db, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
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
    public Map<String, String> show(String id) {
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
    public void add(Map<String, String> data) throws Exception {
        File file = getServerXmlFile();
        if (file == null || !file.exists()) {
            throw new IllegalStateException("server.xml 文件不存在");
        }

        // 检查端口是否已存在
        if (xmlNodeExists(file, "//Connector[@port='" + port + "']")) {
            return;
        }

        // 构建属性
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
        
        addXmlNode(file, "//Service", "Connector", attrs);
    }

    @Override
    public void delete(String id) throws Exception {
        File file = getServerXmlFile();
        deleteXmlNode(file, "//Connector[@port='" + id + "']");
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        File file = getServerXmlFile();
        if (file == null || !file.exists()) {
            throw new IllegalStateException("server.xml 文件不存在");
        }

        Properties attrs = new Properties();
        for (String field : CONNECTOR_FIELDS) {
            String value = data.get(field);
            attrs.setProperty(field, value != null ? value : "");
        }
        attrs.setProperty("port", id);

        updateXmlNode(file, "//Connector[@port='" + id + "']", attrs);
    }


    private java.util.List<Map<String, String>> parseServerXml() {
        java.util.List<Map<String, String>> result = new ArrayList<>();
        try {
            File file = getServerXmlFile();
            if (file == null || !file.exists()) {
                return result;
            }
            
            List<Properties> nodes = getXmlNodes(file, "//Connector");
            
            for (Properties p : nodes) {
                Map<String, String> c1 = new HashMap<>();
                c1.put("id", p.getProperty("port"));
                c1.put("protocol", p.getProperty("protocol", DEFAULT_PROTOCOL));
                c1.put("port", p.getProperty("port"));
                c1.put("connectionTimeout", p.getProperty("connectionTimeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT)));
                c1.put("redirectPort", p.getProperty("redirectPort", ""));
                c1.put("address", p.getProperty("address", DEFAULT_ADDRESS));
                c1.put("maxThreads", p.getProperty("maxThreads", String.valueOf(DEFAULT_MAX_THREADS)));
                c1.put("minSpareThreads", p.getProperty("minSpareThreads", String.valueOf(DEFAULT_MIN_SPARE_THREADS)));
                c1.put("acceptCount", p.getProperty("acceptCount", String.valueOf(DEFAULT_ACCEPT_COUNT)));
                result.add(c1);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }
}
