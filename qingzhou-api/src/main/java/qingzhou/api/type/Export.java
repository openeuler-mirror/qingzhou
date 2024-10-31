package qingzhou.api.type;

import java.io.IOException;
import java.util.Map;

public interface Export {
    String ACTION_EXPORT = "export";

    StreamSupplier exportData(Map<String, String> query);

    interface StreamSupplier {
        byte[] read(long offset) throws IOException;

        long offset();

        default String getDownloadName() {
            return null;
        }
    }
}
