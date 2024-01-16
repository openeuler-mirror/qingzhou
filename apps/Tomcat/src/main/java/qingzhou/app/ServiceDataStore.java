package qingzhou.app;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import qingzhou.framework.api.DataStore;
import qingzhou.framework.util.NativeCommandUtil;
import qingzhou.framework.util.StringCollector;

public class ServiceDataStore implements DataStore {

    private static final String ID_NAME = "name";
    // 注意：这里直接放到内存中，用来代替数据库或配置文件，实际开发中要持久化到文件或数据库中
    private static final List<Map<String, String>> DATAS = new ArrayList<>();

    @Override
    public List<Map<String, String>> getAllData(String type) throws Exception {
        return datas();
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) throws Exception {
        try {
            if (!getData(properties.get(ID_NAME)).isEmpty()) {
                throw new IllegalStateException("Tomcat 服务已存在！");
            } else {
                properties.put("tomcatPort", parseTomcatPort(properties.get("tomcatPath")));
                datas().add(properties);
            }
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Override
    public List<String> getDataIdInPage(String type, int pageSize, int pageNum) throws Exception {
        try {
            List<Map<String, String>> list = findList(null);
            int endIndex = (list.size() >= pageSize * pageNum ?  pageSize * pageNum : list.size()) - 1;
            return list.subList(pageSize * (pageNum - 1), endIndex).stream()
            .flatMap(map -> map.entrySet().stream())
            .filter(entry -> ID_NAME.equals(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) throws Exception {
        try {
            Map<String, String> one = getData(id);
            if (one.isEmpty()) {
                throw new IllegalStateException("Tomcat 服务不存在！");
            }
            data.put("tomcatPort", parseTomcatPort(one.get("tomcatPath")));
            one.forEach((k, v) -> {
                if (data.containsKey(k)) {
                    one.put(k, data.get(k));
                }
            });
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Override
    public void deleteDataById(String type, String id) throws Exception {
        try {
            for (int i = 0; i < datas().size(); i++) {
                if (datas().get(i).get(ID_NAME).equals(id)) {
                    datas().remove(i);
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public Map<String, String> getData(String id) {
        for (Map<String, String> item : datas()) {
            if (item.get(ID_NAME).equals(id)) {
                return item;
            }
        }
        return new HashMap<>();
    }

    public List<Map<String, String>> findList(Map<String, String> whereMap) {
        List<Map<String, String>> result = new ArrayList<>();
        if (whereMap == null || whereMap.isEmpty()) {
            return datas();
        }
        datas().forEach(i -> {
            for (Map.Entry<String, String> entry: i.entrySet()) {
                if (entry.getValue() != null && entry.getValue().equals(whereMap.get(entry.getKey()))) {
                    result.add(i);
                }
            }
        });
        return result;
    }

    public synchronized void startService(String id) throws Exception {
        Map<String, String> one = getData(id);
        if (one.isEmpty()) {
            throw new IllegalStateException("Tomcat 服务不存在！");
        }
        String port = parseTomcatPort(one.get("tomcatPath"));
        if (!isPortAvailable(port)) {
            throw new IllegalStateException("服务已启动或端口号[" + port + "]被占用");
        }
        String cmd = new File(one.get("tomcatPath"), "bin/catalina." + (System.getProperty("os.name").contains("Windows") ? "bat" : "sh")).getPath() + " start";
        System.out.println("[TomcatManagement] To be excuted command:" + cmd);
        StringCollector collector = new StringCollector();
        NativeCommandUtil.runNativeCommand(cmd, new File(one.get("tomcatPath"), "bin"), collector, 60);
        long time = 5000L;
        while (time > 0) {
            try {
                StringCollector sc = new StringCollector();
                NativeCommandUtil.runNativeCommand(System.getProperty("os.name").contains("Windows") ? ("netstat -aon|findstr " + port) : ("netstat -lnp | grep " + port), new File(one.get("tomcatPath"), "bin"), sc,2);
                try (ByteArrayInputStream inStream = new ByteArrayInputStream(sc.destroy().getBytes()); InputStreamReader reader = new InputStreamReader(inStream, "UTF-8"); 
                BufferedReader in = new BufferedReader(reader)) {
                    String l;
                    while ((l = in.readLine()) != null) {
                        if (System.getProperty("os.name").contains("Windows")) {
                            if (l.trim().startsWith("TCP") && l.contains(":" + port) && l.contains(" LISTENING ")) {
                                time = 0;
                                break;
                            }
                        } else {
                            if (l.trim().startsWith("tcp") && l.contains(":" + port) && l.trim().endsWith("/java")) {
                                time = 0;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (time > 0) {
                    time -= 300L;
                    Thread.sleep(300L);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (isPortAvailable(port)) {
            throw new IllegalStateException("启动超时，请稍后重试！");
        }
    }
    
    public synchronized void stopService(String id) throws Exception {
        Map<String, String> one = getData(id);
        if (one.isEmpty()) {
            throw new IllegalStateException("Tomcat 服务不存在！");
        }
        String port = parseTomcatPort(one.get("tomcatPath"));
        if (isPortAvailable(port)) {
            throw new IllegalStateException("服务未启动或端口号[" + port + "]错误");
        }
        String cmd = new File(one.get("tomcatPath"), "bin/shutdown." + (System.getProperty("os.name").contains("Windows") ? "bat" : "sh")).getPath();
        System.out.println("To be excuted command:" + cmd);
        StringCollector collector = new StringCollector();
        NativeCommandUtil.runNativeCommand(cmd, new File(one.get("tomcatPath"), "bin"), collector, 20);
        if (isPortAvailable(port)) {
            throw new IllegalStateException("停止超时，请稍后重试！");
        }
    }

    private synchronized List<Map<String, String>> datas() {
        DATAS.forEach(i -> {
            i.put("tomcatPort", parseTomcatPort(i.get("tomcatPath")));
            i.put("started", String.valueOf(!isPortAvailable(parseTomcatPort(i.get("tomcatPath")))));
            i.put("tomcatVersion", getVersion(i.get("tomcatPath")));
        });
        return DATAS;
    }

    private String parseTomcatPort(String tomcatPath) {
        File configPath = new File(tomcatPath, "conf/server.xml");
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configPath);
            XPath xPath =  XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile("//Server/Service/Connector[@protocol='HTTP/1.1']").evaluate(doc, XPathConstants.NODESET);
            if (nodeList != null && nodeList.getLength() > 0) {
                return ((Element) nodeList.item(0)).getAttribute("port");
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | SAXException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private String getVersion(String tomcatPath) {
        String cmd = new File(tomcatPath, "bin/version." + (System.getProperty("os.name").contains("Windows") ? "bat" : "sh")).getPath();
        try {
            StringCollector collector = new StringCollector();
            NativeCommandUtil.runNativeCommand(cmd, new File(tomcatPath, "bin"), collector, 20);
            List<String> lines = Arrays.asList(collector.destroy().split(System.lineSeparator()));
            for (String line : lines) {
                if (line.contains("Server version: ")) {
                    return line.substring(line.indexOf("Server version:") + "Server version:".length()).trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isPortAvailable(String port) {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
            serverSocket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
}
