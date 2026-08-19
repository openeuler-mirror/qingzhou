package com.tongtech.zookeeper.starter.service;

import com.tongtech.zookeeper.starter.ZookeeperApp;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Monitor;

import java.util.HashMap;
import java.util.Map;

@Model(code = "summary", order = 1, icon = "House",
        name = {"概要", "en:Summary"},
        info = {"展示基础信息。",
                "en:Display summary information."})
public class Summary extends AbstractZkCommandService implements Monitor {

    @ModelField(
            field_type = FieldType.MONITORING,
            name = {"JAVA 主目录", "en:JAVA HOME"})
    public String javaHome;

    @ModelField(
            field_type = FieldType.MONITORING,
            name = {"Zookeeper 目录", "en:Zookeeper Home"})
    public String zookeeperHome;

    @ModelField(
            field_type = FieldType.MONITORING,
            name = {"配置文件", "en:Zookeeper Config"})
    public String zookeeperCfg;

    @ModelField(
            field_type = FieldType.MONITORING,
            name = {"版本号", "en:Zookeeper Version"})
    public String zookeeperVersion;

    @Override
    public Map<String, String> monitor(String s) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("javaHome", System.getProperty("java.home"));
        map.put("zookeeperHome", getAppContext().getDetectedPath());
        map.put("zookeeperCfg", findMetaDataProperties(ZookeeperApp.METADATA_CONFIG_KEY));
        map.put("zookeeperVersion", findMetaDataProperties(ZookeeperApp.METADATA_VERSION_KEY));
        return map;
    }
}
