package qingzhou.app;

import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import java.io.File;
import java.util.Map;

@Model(name = "tomcatService", icon = "folder-open-alt", nameI18n = {"Tomcat 服务管理", "en:Tomcat service management"}, infoI18n = {"Tomcat 服务管理。", "en:Tomcat service management."})
public class TomcatServiceModel extends ModelBase implements AddModel {
    
    @ModelField(showToList = false, showToEdit = false, disableOnCreate = true, disableOnEdit = true, nameI18n = {"主键", "en:ID"}, infoI18n = {"名称。", "en:Name."})
    public String id;
    
    @ModelField(showToList = true, required = true, nameI18n = {"名称", "en:Name"}, infoI18n = {"名称。", "en:Name."})
    public String name;

    @ModelField(showToList = true, nameI18n = {"Tomcat 路径", "en:Tomcat path"}, infoI18n = {"Tomcat 路径", "en:Tomcat path."})
    public String tomcatPath;

    @ModelField(showToList = true, showToEdit = false, disableOnCreate = true, disableOnEdit = true, nameI18n = {"Tomcat 版本", "en:Tomcat version"}, infoI18n = {"Tomcat 版本", "en:Tomcat version."})
    public String tomcatVersion;

    @ModelField(showToList = true, showToEdit = false, disableOnCreate = true, disableOnEdit = true, nameI18n = {"Tomcat 端口号", "en:Tomcat port"}, infoI18n = {"Tomcat 端口号", "en:Tomcat port."})
    public Integer tomcatPort;

    @ModelField(showToList = true, showToEdit = false, disableOnCreate = true, disableOnEdit = true, nameI18n = {"是否启动", "en:Started state."}, infoI18n = {"是否启动", "en:Started state."})
    public Boolean started = false;

    @Override
    public void init() {
        ConsoleContext ctx = getAppContext().getConsoleContext();
        ctx.addI18N("app.tomcat.path.notexist", new String[]{"Tomcat 路径不存在或无法访问", "en:The tomcat home path does not exist or inaccessible."});
    }

    @Override
    public String validate(Request request, String fieldName) {
        if ("tomcatPath".equals(fieldName)) {
            String value = request.getParameter(fieldName);
            if (value != null && !"".equals(value)) {
                if (!new File(value).exists() || new File(value).isFile()) {
                    return getAppContext().getConsoleContext().getI18N("app.tomcat.path.notexist");
                }
            }
        }

        return super.validate(request, fieldName);
    }
    
    @Override
    public void add(Request request, Response response) throws Exception {
        Map<String, String> p = prepareParameters(request);
        getDataStore().addData(request.getModelName(), request.getId(), p);
        response.setSuccess(true);
        response.setMsg("成功");
    }

    @Override
    public void edit(Request request, Response response) throws Exception {
        ServiceDataStore storeSrv = getDataStore();
        response.addData(storeSrv.getData(request.getId()));
        response.setSuccess(true);
        response.setMsg("成功");
    }

    @Override
    public void update(Request request, Response response) throws Exception {
        Map<String, String> p = prepareParameters(request);
        getDataStore().updateDataById(request.getModelName(), request.getId(), p);
        response.setSuccess(true);
        response.setMsg("成功");
    }

    @Override
    public void list(Request request, Response response) throws Exception {
        response.getDataList().addAll(getDataStore().getAllData(request.getModelName()));
        response.setSuccess(true);
        response.setMsg("成功");
    }

    @Override
    public void delete(Request request, Response response) throws Exception {
        getDataStore().deleteDataById(request.getModelName(), request.getId());
        response.setSuccess(true);
        response.setMsg("删除成功！");
    }
    
    @ModelAction(name = "start", icon = "play", showToList = true, effectiveWhen = "started=false", nameI18n = {"启动", "en:Start"}, infoI18n = {"启动 Tomcat 服务。", "en:Start Tomcat service."})
    public void start(Request request, Response response) throws Exception {
        ServiceDataStore storeSrv = getDataStore();
        storeSrv.startService(request.getId());
        response.setSuccess(true);
        response.setMsg("启动成功！");
    }

    @ModelAction(name = "stop", icon = "stop", showToList = true, effectiveWhen = "started=true", nameI18n = {"停止", "en:Stop"}, infoI18n = {"停止 Tomcat 服务。", "en:Stop Tomcat service."})
    public void stop(Request request, Response response) throws Exception {
        ServiceDataStore storeSrv = getDataStore();
        storeSrv.stopService(request.getId());
        response.setSuccess(true);
        response.setMsg("停止成功！");
    }
}
