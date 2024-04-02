package qingzhou.crypto;

import qingzhou.bootstrap.main.ServiceRegister;
import qingzhou.framework.crypto.CryptoService;

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
