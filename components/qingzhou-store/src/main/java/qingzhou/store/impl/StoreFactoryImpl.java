package qingzhou.store.impl;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import qingzhou.store.Store;
import qingzhou.store.StoreFactory;

@Component
public class StoreFactoryImpl implements StoreFactory {
    @Override
    public Store buildMemoryStore() {
        return new MemoryStore();
    }

    @Override
    public Store buildFileStore(File baseDir) {
        return new FileStore(baseDir);
    }
}
