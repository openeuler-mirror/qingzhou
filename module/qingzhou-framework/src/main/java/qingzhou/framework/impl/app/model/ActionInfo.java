package qingzhou.framework.impl.app.model;

import qingzhou.api.console.ModelAction;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ActionInfo {
    public final ModelAction modelAction;
    public final Method javaMethod;
    public final Map<Integer, Class<?>> parameterTypesIndex;

    public ActionInfo(ModelAction modelAction, Method javaMethod) {
        this.modelAction = modelAction;
        this.javaMethod = javaMethod;

        Map<Integer, Class<?>> parameterTypesIndex = new LinkedHashMap<>();
        Class<?>[] parameterTypes = javaMethod.getParameterTypes();
        Class<?>[] supportedTypes = {Request.class, Response.class};
        parameterTypes:
        for (int i = 0; i < parameterTypes.length; i++) {
            for (Class<?> type : supportedTypes) {
                if (type == parameterTypes[i]) { //  不能用 isAssignableFrom，否则在实际调用时候会出现类型不匹配，例如 需要的参数是具体的实现，而实际传入的是父类
                    parameterTypesIndex.put(i, type);
                    continue parameterTypes;
                }
            }
            throw new IllegalArgumentException("The parameter type ("
                    + parameterTypes[i].getSimpleName()
                    + ") of a ModelAction annotated method can only be these: "
                    + Arrays.stream(supportedTypes).map(Class::getSimpleName).collect(Collectors.joining(", "))
            );
        }
        this.parameterTypesIndex = Collections.unmodifiableMap(parameterTypesIndex);
    }
}
