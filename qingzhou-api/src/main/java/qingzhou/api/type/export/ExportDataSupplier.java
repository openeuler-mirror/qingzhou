package qingzhou.api.type.export;

import java.io.IOException;

public interface ExportDataSupplier {
    byte[] read(long offset) throws IOException;

    long offset();

    default String name() {
        return null;
    }
}
