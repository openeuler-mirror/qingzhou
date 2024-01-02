package qingzhou.serializer.impl;

import qingzhou.framework.ServiceRegister;
import qingzhou.serializer.SerializerService;
import qingzhou.serializer.impl.java.JavaSerializer;

public class Controller extends ServiceRegister<SerializerService> {
    @Override
    protected Class<SerializerService> serviceType() {
        return SerializerService.class;
    }

    @Override
    protected SerializerService serviceObject() {
        return JavaSerializer::new;
    }
}
