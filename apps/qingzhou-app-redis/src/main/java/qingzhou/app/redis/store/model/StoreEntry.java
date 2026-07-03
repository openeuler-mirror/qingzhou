package qingzhou.app.redis.store.model;

public interface StoreEntry {
    String getId();
    void setId(String id);
    long getTimestamp();
    void setTimestamp(long timestamp);
}
