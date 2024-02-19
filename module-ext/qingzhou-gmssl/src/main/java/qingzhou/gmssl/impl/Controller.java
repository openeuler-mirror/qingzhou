package qingzhou.gmssl.impl;

import qingzhou.framework.service.ServiceRegister;
import qingzhou.gmssl.GmSSLService;

public class Controller extends ServiceRegister<GmSSLService> {
    @Override
    protected Class<GmSSLService> serviceType() {
        return GmSSLService.class;
    }

    @Override
    protected GmSSLService serviceObject() {
        return new GmSSLService() {
        };
    }
}
