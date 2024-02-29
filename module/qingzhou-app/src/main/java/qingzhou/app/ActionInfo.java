package qingzhou.app;

import qingzhou.api.ModelAction;

import java.lang.reflect.Method;

public class ActionInfo {
    public final ModelAction modelAction;
    public final String methodName;
    private Method javaMethod;

    public ActionInfo(ModelAction modelAction, String methodName) {
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
