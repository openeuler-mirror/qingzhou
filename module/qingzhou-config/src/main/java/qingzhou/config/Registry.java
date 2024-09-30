package qingzhou.config;

public class Registry {
    private long instanceTimeout;
    private long instanceInterval;

    public long getInstanceTimeout() {
        return instanceTimeout;
    }

    public void setInstanceTimeout(long instanceTimeout) {
        this.instanceTimeout = instanceTimeout;
    }

    public long getInstanceInterval() {
        return instanceInterval;
    }

    public void setInstanceInterval(long instanceInterval) {
        this.instanceInterval = instanceInterval;
    }
}
