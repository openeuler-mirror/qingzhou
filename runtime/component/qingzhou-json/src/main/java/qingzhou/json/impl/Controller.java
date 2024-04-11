package qingzhou.json.impl;

import qingzhou.engine.ServiceRegister;
import qingzhou.json.Json;

public class Controller extends ServiceRegister<Json> {
    @Override
    public Class<Json> serviceType() {
        return Json.class;
    }

    @Override
    protected Json serviceObject() {
        return new JsonImpl();
    }
}
