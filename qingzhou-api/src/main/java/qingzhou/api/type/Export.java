package qingzhou.api.type;

import qingzhou.api.type.export.ExportDataSupplier;

public interface Export {
    String ACTION_EXPORT = "export";

    ExportDataSupplier exportData(String id) throws Exception;
}
