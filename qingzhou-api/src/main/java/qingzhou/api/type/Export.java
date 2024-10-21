package qingzhou.api.type;

import java.io.IOException;

public interface Export {
    String ACTION_EXPORT = "export";

    StreamSupplier exportData(String id);

    interface StreamSupplier {
        byte[] read(long offset) throws IOException;

        long offset();
    }
}
