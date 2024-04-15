package qingzhou.registry;

import java.util.Collection;

public interface Registry {
    String PARAMETER_FINGERPRINT = "fingerprint";
    String PARAMETER_DO_REGISTER = "doRegister";

    boolean checkRegistered(String dataFingerprint);

    void register(String registrationData); // 远程注册，数据为 json 格式

    void register(InstanceInfo instanceInfo); // 本地注册

    Collection<String> getAllInstanceId();

    InstanceInfo getInstanceInfo(String id);
}
