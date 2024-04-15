package qingzhou.registry.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.engine.ServiceRegister;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

public class Controller extends ServiceRegister<Registry> {
    @Override
    public Class<Registry> serviceType() {
        return Registry.class;
    }

    @Override
    protected Registry serviceObject() {
        return new RegistryImpl(
                moduleContext.getService(Json.class),
                moduleContext.getService(CryptoService.class).getMessageDigest()
        );
    }
}
