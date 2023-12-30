package qingzhou.crypto.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.framework.ServiceRegister;

public class Controller extends ServiceRegister<CryptoService> {
    @Override
    protected Class<CryptoService> serviceType() {
        return CryptoService.class;
    }

    @Override
    protected CryptoService serviceObject() {
        return new CryptoServiceImpl();
    }
}
