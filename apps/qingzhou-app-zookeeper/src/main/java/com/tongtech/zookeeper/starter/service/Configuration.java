package com.tongtech.zookeeper.starter.service;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.action.Monitor;

import java.util.Map;


@Model(code = "configuration", order = 4, icon = "icon-file-o",
        name = {"生效参数", "en:Validity parameter"},
        info = {"展示生效中配置参数的值。",
                "en:Displays the values of the configuration parameters in effect."})
public class Configuration extends AbstractZkCommandService implements Monitor {

    @ModelField(
            field_type = FieldType.monitor,
            name = {"客户端连接端口", "en:Client Port"})
    public String client_port;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"快照数据目录", "en:Snapshot Data Dir"})
    public String data_dir;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"数据日志目录", "en:Data Log Dir"})
    public String data_log_dir;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"心跳时间", "en:Tick Time"})
    public String tick_time;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"客服端最大连接数", "en:Max Client Cnxns"})
    public String max_client_cnxns;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"会话的最小超时时间", "en:Min Session Timeout"})
    public String min_session_timeout;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"会话的最大超时时间", "en:Max Session Timeout"})
    public String max_session_timeout;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"服务器 id", "en:Server Id"})
    public String server_id;

    @ModelField(
            field_type = FieldType.monitor,
            name = {"客户端端口backlog", "en:Client Port Listen Backlog"})
    public String client_port_listen_backlog;

    @Override
    protected String getSubPath() {
        return "configuration";
    }

    @Override
    public Map<String, String> monitor(String s) throws Exception {
        checkJetty();
        return convertValuesToString(sendHttp());
    }
}
