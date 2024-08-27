package qingzhou.deployer.impl;

import qingzhou.api.Request;
import qingzhou.api.Response;

import java.lang.reflect.Method;

interface ActionMethod {
    void invoke(Request request, Response response) throws Exception;

    static ActionMethod buildActionMethod(String methodName, Object instance) {
        return new ActionMethod() {
            private final Method method;

            {
                try {
                    method = instance.getClass().getMethod(methodName, Request.class, Response.class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void invoke(Request request, Response response) throws Exception {
                method.invoke(instance, request, response);
            }
        };
    }
}
