package qingzhou.engine.util.crypto;

import qingzhou.engine.util.crypto.impl.CryptoServiceImpl;

public class CryptoServiceFactory {
    public static CryptoService getInstance() {
        return new CryptoServiceImpl();
    }
}
