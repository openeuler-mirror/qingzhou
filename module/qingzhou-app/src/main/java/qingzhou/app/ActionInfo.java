package qingzhou.app;

import qingzhou.api.metadata.ModelActionData;

import java.io.Serializable;

public class ActionInfo implements Serializable {
    public final String methodName;
    public final ModelActionData modelAction;

    public final transient InvokeMethod invokeMethod;

    public ActionInfo(ModelActionData modelAction, String methodName, InvokeMethod invokeMethod) {
        this.methodName = methodName;
        this.modelAction = modelAction;
        this.invokeMethod = invokeMethod;
    }

    public interface InvokeMethod {
        void invoke(Object... args) throws Exception;
    }
}
