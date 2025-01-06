package qingzhou.app.master;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.core.DeployerConstants;
import qingzhou.core.registry.Registry;

@Model(code = DeployerConstants.MODEL_REGISTER,
        hidden = true,
        name = {"集中管理", "en:Master"},
        info = {"受理远程轻舟实例的注册等请求。",
                "en:Accept requests for registration of remote Qingzhou instances."})
public class Register extends ModelBase {
    @ModelAction(
            code = DeployerConstants.ACTION_CHECK,
            auth_free = true,
            name = {"注册检查", "en:Check Registry"},
            info = {"用于接收实例心跳信息。", "en:Used to receive the heartbeat information of the instance."})
    public void check(Request request) {
        String fingerprint = request.getParameter(DeployerConstants.CHECK_FINGERPRINT);
        if (fingerprint != null) {
            Registry registry = Main.getService(Registry.class);
            request.getResponse().setData(String.valueOf(registry.checkRegistry(fingerprint)));
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_REGISTER,
            auth_free = true,
            name = {"注册实例", "en:Register"},
            info = {"用于接收实例注册的信息。", "en:Information used to receive instance registrations."})
    public void register(Request request) {
        String doRegister = request.getParameter(DeployerConstants.DO_REGISTER);
        if (doRegister != null) {
            Registry registry = Main.getService(Registry.class);
            registry.register(doRegister);
        }
    }
}
