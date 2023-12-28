package qingzhou.console.master.security;

import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.ListModel;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;

@Model(name = Constants.MODEL_NAME_auditlog, icon = "search",
        menuName = "Security", menuOrder = 6,
        nameI18n = {"审计日志", "en:Audit Log"},
        infoI18n = {"TongWeb 服务器的审计日志，用于跟踪记录用户对服务器所进行的操作。", "en:The audit log of the TongWeb server is used to keep track of the actions performed by users on the server."})
public class AuditLog extends MasterModelBase implements ListModel {
    @ModelField(showToList = true, nameI18n = {"id", "en:Id"}, infoI18n = {"Id。", "en:Id."})
    public String id;

    @ModelField(showToList = true, nameI18n = {"操作时间", "en:Operation Time"}, infoI18n = {"操作时间。", "en:Operation time."})
    public String time;

    @ModelField(showToList = true, nameI18n = {"管理员", "en:User"}, infoI18n = {"进行该操作的管理员。", "en:The name of the user who performed the operation."})
    public String user;

    @ModelField(showToList = true, nameI18n = {"操作对象", "en:Operation Object"}, infoI18n = {"组件名。", "en:Model name."})
    public String model;

    @ModelField(showToList = true, nameI18n = {"操作类型", "en:Operation Type"}, infoI18n = {"操作名。", "en:Action name."})
    public String action;

    @ModelField(showToList = true, nameI18n = {"操作结果", "en:Operation Result"}, infoI18n = {"操作结果。", "en:Action result."})
    public String result;

    @ModelField(showToList = true, nameI18n = {"请求地址", "en:Request URI"}, infoI18n = {"请求地址。", "en:Request uri."})
    public String requestUri;

    @ModelField(showToList = true, nameI18n = {"客户端IP", "en:Client IP"}, infoI18n = {"客户端IP。", "en:Client ip."})
    public String clientIp;

    @Override
    public void list(Request request, Response response) throws Exception {
    }
}
