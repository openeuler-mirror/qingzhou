package qingzhou.deployer.impl;

import java.lang.reflect.Method;

import qingzhou.api.Request;

interface ActionMethod {
    void invoke(Request request) throws Exception;

    static ActionMethod buildActionMethod(Method method, Object instance) {
        return request -> method.invoke(instance, request);
    }
}
