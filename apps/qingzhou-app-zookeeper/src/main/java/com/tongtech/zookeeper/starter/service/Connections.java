package com.tongtech.zookeeper.starter.service;

import com.tongtech.zookeeper.starter.ZookeeperApp;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.List;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Model(code = "connections", order = 3, icon = "icon icon-minus",
        name = {"连接", "en:Connections"},
        info = {"关于客户端到服务器连接的信息。",
                "en:Information on client connections to server."})
public class Connections extends AbstractZkCommandService implements Monitor, List, Show {

    private static final String ID_KEY = "remote_socket_address";

    @ModelField(
            name = {"远端连接地址", "en:Remote Socket Address"})
    public String remote_socket_address;

    @ModelField(
            name = {"会话超时时间", "en:Session Timeout"})
    public String session_timeout;

    @ModelField(
            name = {"感兴趣的操作", "en:Interest Ops"})
    public String interest_ops;

    @ModelField(
            name = {"是否临时", "en:Ephemeral Owner"})
    public String ephemeral_owner;

    @ModelField(
            name = {"最后执行的操作", "en:Last Operation"})
    public String last_operation;

    @ModelField(
            name = {"连接开始时间", "en:Session Established"})
    public String session_established;

    @ModelField(
            name = {"最后执行操作时间", "en:Last Operation Time"})
    public String last_operation_time;

    @ModelField(
            name = {"连接所在 IP", "en:Local Socket Address"})
    public String local_socket_address;

    @ModelField(
            name = {"最后执行的响应时间", "en:Last Response Time"})
    public String last_response_time;

    @ModelField(
            name = {"最后延迟", "en:Last Latency"})
    public String last_latency;

    @ModelField(
            name = {"连接超时时间", "en:Outstanding Requests"})
    public String outstanding_requests;

    @ModelField(
            name = {"操作计数", "en:Ops Count"})
    public String ops_count;

    @ModelField(
            name = {"最后 Cxid", "en:Last Cxid"})
    public String last_cxid;

    @ModelField(
            name = {"最后 Zxid", "en:Last Zxid"})
    public String last_zxid;

    @ModelField(
            name = {"发送数据量", "en:Sent"})
    public String sent;

    @ModelField(
            name = {"接收数据量", "en:Received"})
    public String received;

    @Override
    protected String getSubPath() {
        return "connections";
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        checkJetty();
        String decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8.name());
        Map<String, Object> connection = findConnection(decodedId);
        if (connection == null) {
            return Collections.emptyMap();
        }
        return convertValuesToString(connection);
    }

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] showFields) throws Exception {
        java.util.List<String> allIds = allIds();
        if (allIds.isEmpty()) {
            return new ArrayList<>();
        }
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allIds.size());
        java.util.List<String> pageIds = allIds.subList(startIndex, endIndex);

        java.util.List<String[]> data = new ArrayList<>();
        for (String connectionId : pageIds) {
            Map<String, String> connectionData = show(connectionId);
            String[] row = new String[showFields.length];
            for (int i = 0; i < showFields.length; i++) {
                row[i] = connectionData.get(showFields[i]);
            }
            data.add(row);
        }
        return data;
    }

    @Override
    public Map<String, String> monitor(String s) throws Exception {
        return show(s);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        try {
            return allIds().size();
        } catch (Exception e) {
            ZookeeperApp.logger.error("Failed to get connection total size", e);
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            return !show(id).isEmpty();
        } catch (Exception e) {
            ZookeeperApp.logger.error("Failed to check connection: " + id, e);
            return false;
        }
    }

    private java.util.List<String> allIds() throws Exception {
        checkJetty();
        java.util.List<String> ids = new ArrayList<>();
        for (Map<String, Object> connection : listConnections()) {
            Object address = connection.get(ID_KEY);
            if (address != null) {
                ids.add(String.valueOf(address));
            }
        }
        return ids;
    }

    private Map<String, Object> findConnection(String decodedId) throws Exception {
        for (Map<String, Object> connection : listConnections()) {
            if (decodedId.equals(String.valueOf(connection.get(ID_KEY)))) {
                return connection;
            }
        }
        return null;
    }

    /** 解析 admin server 返回的 connections 列表 */
    @SuppressWarnings("unchecked")
    private java.util.List<Map<String, Object>> listConnections() throws Exception {
        Map<String, Object> response = sendHttp();
        Object connections = response.get("connections");
        if (!(connections instanceof java.util.List)) {
            return Collections.emptyList();
        }
        java.util.List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (java.util.List<?>) connections) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }
}
