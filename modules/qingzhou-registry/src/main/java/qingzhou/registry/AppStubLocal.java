package qingzhou.registry;

import qingzhou.api.AppContext;

public interface AppStubLocal extends AppStub {
    AppContext getAppContext();
}
