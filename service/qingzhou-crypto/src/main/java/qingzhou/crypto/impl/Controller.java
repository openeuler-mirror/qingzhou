package qingzhou.crypto.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.framework.service.ServiceRegister;

public class Controller extends ServiceRegister<CryptoService> {
    private final CryptoService cryptoService = new CryptoServiceImpl();

    @Override
    protected Class<CryptoService> serviceType() {
        return CryptoService.class;
    }

    @Override
    protected CryptoService serviceObject() {
        return cryptoService;
    }
}
