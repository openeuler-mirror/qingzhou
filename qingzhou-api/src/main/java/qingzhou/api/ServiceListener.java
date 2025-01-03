package qingzhou.api;

/**
 * 保持一致：qingzhou.engine.ServiceListener
 */
public interface ServiceListener {
    void onServiceEvent(ServiceEvent event, Class<?> serviceType);

    enum ServiceEvent {
        // 保持一致：qingzhou.engine.ServiceListener.ServiceEvent
        REGISTERED, GOT
    }
}
