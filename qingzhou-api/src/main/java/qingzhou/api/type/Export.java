package qingzhou.api.type;

import java.io.IOException;

public interface Export {
    String ACTION_EXPORT = "export";

    DataSupplier exportData(String id) throws Exception;

    interface DataSupplier {
        byte[] read(long offset) throws IOException;

        long offset();

        default String name() {
            return null;
        }
    }
}
