package qingzhou.app.master.service;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Editable;
import qingzhou.app.master.MasterApp;
import qingzhou.registry.Registry;

import java.util.HashMap;
import java.util.Map;

@Model(code = "heartservice", icon = "heart",
        menu = "Service", order = 4,
        hidden = true,
        entrance = Editable.ACTION_NAME_EDIT,
        name = {"心跳服务", "en:Heartbeat Service"},
        info = {"用于接收实例上报的心跳及已部署的应用信息。", "en:It is used to receive the heartbeat and deployed application information reported by the instance."})
public class HeartBeat extends ModelBase {
    @ModelAction(
            name = {"心跳", "en:Heatbeat"},
            info = {"用于接收实例心跳信息。", "en:Used to receive the heartbeat information of the instance."})
    public void heatbeat(Request request, Response response) throws Exception {
        String fingerprint = request.getParameter("fingerprint");
        if (fingerprint != null) {
            Map<String, String> result = new HashMap<>();
            Registry registry = MasterApp.getService(Registry.class);
            result.put(fingerprint, String.valueOf(registry.checkRegistered(fingerprint)));
            response.addData(result);
        }
    }

    @ModelAction(
            name = {"实例注册", "en:Instance Registration"},
            info = {"用于接收实例注册的信息。", "en:Information used to receive instance registrations."})
    public void register(Request request, Response response) throws Exception {
        String doRegister = request.getParameter("doRegister");
        if (doRegister != null) {
            Registry registry = MasterApp.getService(Registry.class);
            registry.register(doRegister);
        }
    }


}
