package qingzhou.engine;

public interface ServiceListener {
    void onServiceEvent(ServiceEvent event, Class<?> serviceType);

    enum ServiceEvent {
        REGISTERED, GOT
    }
}
