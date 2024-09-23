package qingzhou.app.test;

import qingzhou.api.*;
import qingzhou.api.type.Monitorable;
import qingzhou.api.type.Updatable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Model(code = "push_parameter", icon = "pencil",
        entrance = Updatable.ACTION_EDIT, order = 1,
        name = {"推送参数", "en:Push Parameter"},
        info = {"配置推送参数。", "en:Configure the push parameter."})
public class TestConfig extends ModelBase implements Updatable, Monitorable {
    private final Map<String, String> data = new HashMap<>();

    final String tab_SMTP = "SMTP";
    final String tab_DingTalk = "DingTalk";
    final String tab_WeChat = "WeChat";
    final String tab_Alibaba = "AlibabaSMS";
    final String tab_Tencent = "TencentSMS";
    final String tab_WeChatPublic = "WeChatPublic";
    final String tab_HTTPPush = "HTTPPush";

    @ModelField(group = tab_WeChat,
            name = {"发送者邮箱", "en:Sender Email"},
            info = {"发送者邮箱。", "en:The sender email address."})
    public String mailbox;

    @ModelField(group = tab_Tencent,
            name = {"告警静默", "en:SMTP Silence"},
            info = {"告警是否使用静默模式。", "en:Whether the alarm uses silent mode."})
    public Boolean smtpSilence = false;

    @ModelField(
            group = tab_WeChatPublic,

            name = {"aaa", "en:aaa"}, info = {})
    public String aa;
    @ModelField(

            name = {"bbb", "en:bbb"}, info = {})
    public int bb;
    @ModelField(
            group = tab_HTTPPush,

            name = {"ccc", "en:ccc"}, info = {})
    public float cc;

    @ModelField(
            type = FieldType.bool,

            name = {"bool", "en:bool"}, info = {})
    public boolean bool;
    @ModelField(
            type = FieldType.checkbox,
            options = {"a", "b", "c", "d", "e"},
            name = {"checkbox", "en:checkbox"}, info = {})
    public boolean checkbox;
    @ModelField(
            type = FieldType.datetime,

            name = {"datetime", "en:datetime"}, info = {})
    public boolean datetime;
    @ModelField(
            type = FieldType.decimal,

            name = {"decimal", "en:decimal"}, info = {})
    public boolean decimal;
    @ModelField(
            type = FieldType.kv,

            name = {"kv", "en:kv"}, info = {})
    public boolean kv;
    @ModelField(
            type = FieldType.multiselect,
            options = {"a", "b", "c", "d", "e"},
            name = {"multiselect", "en:multiselect"}, info = {})
    public boolean multiselect;
    @ModelField(
            type = FieldType.radio,
            options = {"a", "b", "c", "d", "e"},
            name = {"radio", "en:radio"}, info = {})
    public boolean radio;
    @ModelField(
            type = FieldType.sortable,
            options = {"a", "b", "c", "d", "e"},
            name = {"sortable", "en:sortable"}, info = {})
    public boolean sortable;
    @ModelField(
            type = FieldType.sortablecheckbox,
            options = {"a", "b", "c", "d", "e"},
            name = {"sortablecheckbox", "en:sortablecheckbox"}, info = {})
    public boolean sortablecheckbox;

    @Override
    public Groups groups() {
        return Groups.of(
                Group.of(tab_SMTP),
                Group.of(tab_DingTalk, new String[]{"钉钉群机器人", "en:DingTalk"}),
                Group.of(tab_WeChat, new String[]{"企业微信群机器人", "en:WeChat"}),
                Group.of(tab_Alibaba, new String[]{"阿里云短信", "en:Alibaba SMS"}),
                Group.of(tab_Tencent, new String[]{"腾讯云短信", "en:Tencent SMS"}),
                Group.of(tab_WeChatPublic, new String[]{"微信公众号", "en:WeChat Public"}),
                Group.of(tab_HTTPPush, new String[]{"HTTP推送", "en:HTTP Push"})
        );
    }

    @Override
    public void updateData(Map<String, String> data) {
        this.data.putAll(data);
    }

    @Override
    public Map<String, String> showData(String id) {
        return data;
    }

    @Override
    public Map<String, String> monitorData(String id) {
        return new HashMap<String, String>() {{
            Random random = new Random(100);
            put("aa", String.valueOf(random.nextInt()));
            put("bb", String.valueOf(random.nextInt()));
            put("cc", String.valueOf(random.nextInt()));
        }};
    }
}
