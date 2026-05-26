package qingzhou.agent.embedded.spi;

/**
 * Extension point for customizing agent behavior.
 * Implementations are discovered via ServiceLoader.
 */
public interface AgentPlugin {
    /**
     * Called during agent initialization, before services are started.
     */
    void beforeInit();

    /**
     * Called after all services have been started.
     */
    void afterStart();

    /**
     * Called during agent shutdown.
     */
    void onShutdown();
}