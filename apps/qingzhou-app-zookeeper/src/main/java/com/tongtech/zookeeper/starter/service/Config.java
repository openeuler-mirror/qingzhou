package com.tongtech.zookeeper.starter.service;

import com.tongtech.zookeeper.starter.ZookeeperApp;
import com.tongtech.zookeeper.starter.util.Util;
import qingzhou.api.FieldType;
import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Monitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;


@Model(code = "config", order = 5, icon = "icon-file-text-o",
        name = {"配置", "en:Config"},
        info = {"zookeeper 配置。", "en:zookeeper Configuration."})
public class Config extends ModelBase implements Monitor {
    public static final String OTHER_SP = ";";
    public static final String BASIC = "zoo.cfg";
    private static final Set<String> FIELD_NAME_SET = new HashSet<>();

    static {
        Field[] declaredFields = Config.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (Modifier.isStatic(declaredField.getModifiers())) {
                continue;
            }
            String name = declaredField.getName();
            FIELD_NAME_SET.add(name);
        }
    }


    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            input_type = InputType.number,
            min = 1,
            name = {"心跳时间", "en:Tick Time"},
            info = {"通信心跳时间，Zookeeper服务器与客户端心跳时间，单位毫秒。", "en:Communication heartbeat time， Heartbeat time between the Zookeeper server and the client, in milliseconds."}
    )
    public int tickTime = 2000;

    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            input_type = InputType.number,
            min = 1,
            name = {"LF初始通信时限", "en:Init Limit"},
            info = {"Leader和Follower初始连接时能容忍的最多心跳数，单位次（即tickTime的数量）。", "en:The maximum number of heartbeats (tickTime) that the Leader and Follower can tolerate during the initial connection."})
    public int initLimit = 10;

    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            input_type = InputType.number,
            min = 1,
            name = {"LF同步通信时限", "en:Sync Limit"},
            info = {"Leader和Follower初始连接时能容忍的最多心跳数，单位次（即tickTime的数量）。", "en:The maximum number of heartbeats (tickTime) that the Leader and Follower can tolerate during the initial connection."})
    public int syncLimit = 10;


    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            name = {"快照数据目录", "en:Snapshot Data Dir"}, info = {"快照所在的目录。", "en:the directory where the snapshot is stored."})
    public String dataDir;

    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            port = true, required = true,
            name = {"客户端连接端口", "en:Client Port"})
    public int clientPort;

    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            name = {"myid", "en:MyID"}
    )
    public String myid;


    @ModelField(
            group = BASIC,
            field_type = FieldType.MONITORING,
            separator = OTHER_SP, name = {"其他配置信息", "en:Other Config Info"}, info = {"key=value形式，多个换行写。", "en:The value is in the form of key=value, with multiple line breaks."})
    public String other;


    @Override
    public Map<String, String> monitor(String s) throws Exception {
        Map<String, String> monitorMap = new HashMap<>();
        Map<String, String> zooConfigAsMap = getZooConfigAsMap();
        for (String filedName : FIELD_NAME_SET) {
            monitorMap.put(filedName, zooConfigAsMap.remove(filedName));
        }
        String myid = getAppContext().getProperties().getProperty(ZookeeperApp.MYID_KEY);
        if (Util.notBlank(myid)) {
            monitorMap.put("myid", myid);
        }
        if (!zooConfigAsMap.isEmpty()) {
            String other = ZookeeperApp.json.toJson(zooConfigAsMap);
            monitorMap.put("other", other);
        }
        return monitorMap;
    }

    public Map<String, String> getZooConfigAsMap() {
        Properties properties = (Properties) getAppContext().getProperties().get(ZookeeperApp.ZOO_KEY);

        if (properties == null) {
            return Collections.emptyMap();  // 防御性处理
        }

        return Util.prop2Map(properties);
    }
}
