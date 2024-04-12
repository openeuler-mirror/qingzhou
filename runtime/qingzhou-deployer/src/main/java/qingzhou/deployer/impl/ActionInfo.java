package qingzhou.deployer.impl;

import java.io.Serializable;

public class ActionInfo implements Serializable {
    public final String methodName;
    public final ModelActionDataImpl modelAction;

    public final transient InvokeMethod invokeMethod;

    public ActionInfo(ModelActionDataImpl modelAction, String methodName, InvokeMethod invokeMethod) {
        this.methodName = methodName;
        this.modelAction = modelAction;
        this.invokeMethod = invokeMethod;
    }

    public interface InvokeMethod {
        void invokeMethod(Object... args) throws Exception;
    }
}
