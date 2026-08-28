package com.tongtech.zookeeper.starter.service;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.action.Monitor;

import java.util.HashMap;
import java.util.Map;


@Model(code = "serverstats", order = 2, icon = "icon-server",
        name = {"服务状态", "en:Server Stats"},
        info = {"展示服务运行情况。",
                "en:Show how the service is running."})
public class ServerStats extends AbstractZkCommandService implements Monitor {

    @ModelField(
            field_type = FieldType.monitor,
            name = {"只读", "en:Read Only"})
    public String read_only;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"节点总数", "en:Node Count"})
    public String node_count;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"快照目录大小", "en:Data Dir Size"})
    public String data_dir_size;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"事务日志目录大小", "en:Log Dir Size"})
    public String log_dir_size;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"最后Zxid", "en:Last Processed Zxid"})
    public String last_processed_zxid;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"服务模式", "en:Server Stat"})
    public String server_state;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"已建立的客户端连接数", "en:Number of established client connections"})
    public String num_alive_client_connections;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"最大延迟", "en:Max Latency"})
    public String max_latency;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"平均延迟", "en:Avg Latency"})
    public String avg_latency;

    @Override
    protected String getSubPath() {
        return "server_stats";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> monitor(String id) throws Exception {
        checkJetty();
        Map<String, Object> map = sendHttp();
        Map<String, Object> clientResponse = toMap(map.remove("client_response"));
        Map<String, Object> serverStats = toMap(map.remove("server_stats"));
        serverStats.remove("client_response_stats");
        map.putAll(clientResponse);
        map.putAll(serverStats);
        return convertValuesToString(map);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
}
