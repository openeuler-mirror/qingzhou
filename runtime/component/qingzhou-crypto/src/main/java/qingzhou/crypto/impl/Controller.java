package qingzhou.crypto.impl;

import qingzhou.engine.ServiceRegister;
import qingzhou.crypto.CryptoService;

public class Controller extends ServiceRegister<CryptoService> {
    @Override
    public Class<CryptoService> serviceType() {
        return CryptoService.class;
    }

    @Override
    protected CryptoService serviceObject() {
        return new CryptoServiceImpl();
    }
}
