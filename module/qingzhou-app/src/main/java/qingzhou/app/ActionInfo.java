package qingzhou.app;

import qingzhou.api.metadata.ModelActionData;

import java.io.Serializable;
import java.lang.reflect.Method;

public class ActionInfo implements Serializable {
    public final ModelActionData modelAction;
    public final String methodName;
    private transient Method javaMethod;

    public ActionInfo(ModelActionData modelAction, String methodName) {
        this.modelAction = modelAction;
        this.methodName = methodName;
    }

    public Method getJavaMethod() {
        return javaMethod;
    }

    public void setJavaMethod(Method javaMethod) {
        if (this.javaMethod != null) throw new IllegalStateException();
        this.javaMethod = javaMethod;
    }
}
