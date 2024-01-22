package qingzhou.serializer.impl;

import qingzhou.framework.ServiceRegister;
import qingzhou.serializer.SerializerService;
import qingzhou.serializer.impl.java.JavaSerializer;

public class Controller extends ServiceRegister<SerializerService> {
    private final SerializerService serializerService = JavaSerializer::new;

    @Override
    protected Class<SerializerService> serviceType() {
        return SerializerService.class;
    }

    @Override
    protected SerializerService serviceObject() {
        return serializerService;
    }
}
