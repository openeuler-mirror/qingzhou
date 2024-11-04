package qingzhou.deployer.impl.dashboard;

import qingzhou.api.dashboard.Basic;

import java.util.HashMap;
import java.util.Map;

public class BasicImpl extends DataTypeImpl implements Basic {
    private Map<String, String> data = new HashMap<>();

    @Override
    public Basic put(String key, String value) {
        data.put(key, value);
        return this;
    }

    public Map<String, String> getData() {
        return data;
    }
}
