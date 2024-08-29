package qingzhou.deployer.impl;

import qingzhou.api.Request;

import java.lang.reflect.Method;

interface ActionMethod {
    void invoke(Request request) throws Exception;

    static ActionMethod buildActionMethod(Method method, Object instance) {
        return request -> method.invoke(instance, request);
    }
}
