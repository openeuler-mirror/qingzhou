package qingzhou.api;

import java.io.IOException;

public interface ByteStreamSupplier {
    byte[] read(long offset) throws IOException;

    long offset();

    default String getSupplierName() {
        return null;
    }
}
