package qingzhou.logger;

import qingzhou.engine.Service;

@Service(name = "Log Factory", description = "A simple log factory.")
public interface LogService {

    Logger getLogger(Class<?> clazz);

    void remove(ClassLoader classLoader);
}
