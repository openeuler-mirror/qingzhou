package qingzhou.store;

import java.io.File;

public interface StoreFactory {
    Store buildMemoryStore();

    Store buildFileStore(File baseDir);
}
