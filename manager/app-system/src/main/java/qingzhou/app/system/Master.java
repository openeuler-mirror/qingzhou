package qingzhou.app.system;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.deployer.DeployerConstants;
import qingzhou.registry.Registry;

@Model(code = DeployerConstants.MODEL_MASTER,
        hidden = true,
        name = {"集中管理", "en:Master"},
        info = {"受理远程轻舟实例的注册等请求。",
                "en:Accept requests for registration of remote Qingzhou instances."})
public class Master extends ModelBase {
    @ModelAction(
            code = DeployerConstants.ACTION_CHECK,
            name = {"注册检查", "en:Check Registry"},
            info = {"用于接收实例心跳信息。", "en:Used to receive the heartbeat information of the instance."})
    public void check(Request request) {
        String fingerprint = request.getNonModelParameter(DeployerConstants.CHECK_FINGERPRINT);
        if (fingerprint != null) {
            Map<String, String> result = new HashMap<>();
            Registry registry = Main.getService(Registry.class);
            result.put(fingerprint, String.valueOf(registry.checkRegistry(fingerprint)));
            request.getResponse().addData(result);
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_REGISTER,
            name = {"注册实例", "en:Register"},
            info = {"用于接收实例注册的信息。", "en:Information used to receive instance registrations."})
    public void register(Request request) {
        String doRegister = request.getNonModelParameter("doRegister");
        if (doRegister != null) {
            Registry registry = Main.getService(Registry.class);
            registry.register(doRegister);
        }
    }
}
