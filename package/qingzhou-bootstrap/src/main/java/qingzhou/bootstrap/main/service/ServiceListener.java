package qingzhou.bootstrap.main.service;

public interface ServiceListener {
    void serviceRegistered(Class<?> serviceType);

    void serviceUnregistered(Class<?> serviceType, Object serviceObj);
}
