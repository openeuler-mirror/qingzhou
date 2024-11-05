package qingzhou.engine;

public interface ServiceInfo {
    default boolean isAppShared() {
        return true;
    }

    default String getName() {
        return getClass().getSimpleName();
    }

    default String getDescription() {
        return this.getClass().getName();
    }
}
