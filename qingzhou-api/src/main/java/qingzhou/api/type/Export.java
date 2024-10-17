package qingzhou.api.type;

import java.io.IOException;

public interface Export {
    String ACTION_EXPORT = "export";

    StreamSupplier exportData(String id);

    interface StreamSupplier {
        int read(byte[] block, long offset) throws IOException;

        String serialKey();

        void finished();
    }
}
