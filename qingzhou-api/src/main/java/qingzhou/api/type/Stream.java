package qingzhou.api.type;

import java.io.IOException;

public interface Stream {
    String ACTION_STREAM = "stream";

    StreamSupplier downloadStream(String id);

    interface StreamSupplier {
        int read(byte[] block, long offset) throws IOException;

        String downloadKey();

        void finished();
    }
}
