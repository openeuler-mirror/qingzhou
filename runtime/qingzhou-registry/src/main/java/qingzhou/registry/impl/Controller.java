package qingzhou.registry.impl;

import qingzhou.engine.ServiceRegister;
import qingzhou.registry.Registry;

public class Controller extends ServiceRegister<Registry> {
    @Override
    public Class<Registry> serviceType() {
        return Registry.class;
    }

    @Override
    protected Registry serviceObject() {
        return new RegistryImpl(moduleContext);
    }
}
