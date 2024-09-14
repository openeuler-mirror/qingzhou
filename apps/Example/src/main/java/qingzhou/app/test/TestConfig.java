package qingzhou.app.test;

import qingzhou.api.*;
import qingzhou.api.type.Updatable;

import java.util.Collections;
import java.util.Map;

@Model(code = "push_parameter", icon = "pencil",
        entrance = Updatable.ACTION_EDIT,
        name = {"推送参数", "en:Push Parameter"},
        info = {"配置推送参数。", "en:Configure the push parameter."})
public class TestConfig extends ModelBase implements Updatable {
    final String tab_SMTP = "SMTP";
    final String tab_DingTalk = "DingTalk";
    final String tab_WeChat = "WeChat";
    final String tab_Alibaba = "AlibabaSMS";
    final String tab_Tencent = "TencentSMS";
    final String tab_WeChatPublic = "WeChatPublic";
    final String tab_HTTPPush = "HTTPPush";

    @ModelField(group = tab_SMTP,
            name = {"发送者邮箱", "en:Sender Email"},
            info = {"发送者邮箱。", "en:The sender email address."})
    public String mailbox;

    @ModelField(group = tab_SMTP,
            name = {"告警静默", "en:SMTP Silence"},
            info = {"告警是否使用静默模式。", "en:Whether the alarm uses silent mode."})
    public Boolean smtpSilence = false;

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
    public void updateData(Map<String, String> data) throws Exception {

    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        return Collections.emptyMap();
    }
}
