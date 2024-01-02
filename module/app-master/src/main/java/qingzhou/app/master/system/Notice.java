package qingzhou.app.master.system;

import qingzhou.framework.api.*;

@Model(name = "notice", icon = "bell-alt",
        menuName = "Basic", menuOrder = 4,
        nameI18n = {"系统通知", "en:System Notice"},
        infoI18n = {"通知用于给管理员提供关于 QingZhou 需要重启、授权到期等方面的提示信息。当有系统配置属性改变，服务器需要重新启动才能生效时，系统会给出一条通知，其“详情”一般表示具体的发送变化的属性名，以及其变化前后的值。若变化的属性有多个，则会以分号分隔来表示。此外，属性发生变化后，若再次编辑将其恢复为启动之初的值，则重启TongWeb通知会取消。",
                "en:Notifications are used to provide administrators with information about QingZhou restarts, licence expiry, etc. When a system configuration attribute is changed and the server needs to be restarted for the change to take effect, a notification is given with the \"details\" generally indicating the specific attribute name that was sent for the change and its value before and after the change. If more than one property has changed, they will be separated by a semicolon. In addition, the restart of QingZhou notification will be cancelled if the property is edited again after it has changed to return it to the value it had at the start."})
public class Notice extends ModelBase implements ListModel {
    @ModelField(
            required = true, showToList = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;

    @ModelField(showToList = true, nameI18n = {"组件", "en:Model"}, infoI18n = {"发出该通知的组件名。", "en:The name of the model that issued the notice."})
    public String model;

    @ModelField(nameI18n = {"组件", "en:Model"}, infoI18n = {"发出该通知的组件名。", "en:The name of the model that issued the notice."})
    public String modelName;

    @ModelField(showToList = true, nameI18n = {"消息", "en:Message"}, infoI18n = {"通知的概要信息。", "en:Summary information of the notification."})
    public String msg;

    @ModelField(showToList = true, nameI18n = {"细节", "en:Detail"}, infoI18n = {"通知的细节信息。", "en:The details of the notification."})
    public String detail;

    @Override
    public void init() {
        super.init();
        ConsoleContext master = getAppContext().getConsoleContext();
        master.addI18N("NOTICE_TYPE_RESTART", new String[]{"有配置项发生变更，需重启 QingZhou 以使其生效",
                "en:If a configuration item is changed, QingZhou needs to be restarted for it to take effect"});
    }
}
