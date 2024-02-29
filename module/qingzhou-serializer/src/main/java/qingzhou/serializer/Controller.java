package qingzhou.serializer;

import qingzhou.bootstrap.main.service.ServiceRegister;
import qingzhou.framework.serializer.Serializer;
import qingzhou.serializer.java.JavaSerializer;

public class Controller extends ServiceRegister<Serializer> {
    @Override
    public Class<Serializer> serviceType() {
        return Serializer.class;
    }

    @Override
    protected Serializer serviceObject() {
        return new JavaSerializer();
    }
}
