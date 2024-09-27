package qingzhou.config;

public class Registry {
    private long checkTimeout;
    private long periodicCheckInterval;

    public long getCheckTimeout() {
        return checkTimeout;
    }

    public void setCheckTimeout(long checkTimeout) {
        this.checkTimeout = checkTimeout;
    }

    public long getPeriodicCheckInterval() {
        return periodicCheckInterval;
    }

    public void setPeriodicCheckInterval(long periodicCheckInterval) {
        this.periodicCheckInterval = periodicCheckInterval;
    }
}
