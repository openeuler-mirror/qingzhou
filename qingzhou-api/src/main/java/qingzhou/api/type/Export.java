package qingzhou.api.type;

import qingzhou.api.ByteStreamSupplier;

import java.util.Map;

public interface Export {
    String ACTION_EXPORT = "export";

    ByteStreamSupplier exportData(String id, Map<String, String> query);
}
