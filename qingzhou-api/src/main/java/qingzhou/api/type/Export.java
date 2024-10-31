package qingzhou.api.type;

import qingzhou.api.ByteStreamSupplier;

public interface Export {
    String ACTION_EXPORT = "export";

    ByteStreamSupplier exportData(String id);
}
