package qingzhou.console.login;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.ServerXml;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.TimeUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LockOutRealm {
    /**
     * If a failed user is removed from the cache because the cache is too big
     * before it has been in the cache for at least this period of time (in
     * seconds) a warning message will be logged. Defaults to 3600 (1 hour).
     */
    private final int cacheRemovalWarningTime = 3600;

    /**
     * 单个 ip 登录失败的用户数限制，避免同一个ip进行用户爆破
     */
    private final int cacheSize = 100;

    /**
     * Users whose last authentication attempt failed. Entries will be ordered
     * in access order from least recent to most recent.
     */
    private Map<String, LockRecord> failedUsers;

    LockOutRealm() {
        init();
    }

    public LockRecord getLockRecord(String user) {
        return failedUsers.get(user);
    }

    public int getLockOutTime() {
        int lockOutTime = 300;
        try {
            String count = ServerXml.get().lockOutTime();
            if (StringUtil.notBlank(count)) {
                lockOutTime = Integer.parseInt(count);
            }
        } catch (Exception e) {
            ConsoleUtil.error("Failed to get console lockOutTime", e);
        }
        return lockOutTime;
    }

    public int getFailureCount() {
        int failureCount = 5;
        try {
            String count = ServerXml.get().failureCount();
            if (StringUtil.notBlank(count)) {
                failureCount = Integer.parseInt(count);
            }
        } catch (Exception e) {
            ConsoleUtil.error("Failed to get console failureCount", e);
        }
        return failureCount;
    }

    /*
     * Filters authenticated principals to ensure that <code>null</code> is
     * returned for any user that is currently locked out.
     */
    public boolean filterLockedAccounts(String username, boolean loggedOk) {
        // Register all failed authentications
        if (!loggedOk) {
            registerAuthFailure(username);
        }

        if (isLocked(username)) {
            // If the user is currently locked, authentication will always fail
            // LOGGER.warn("An attempt was made to authenticate the locked user [" + username + "]");
            return false;
        }

        if (loggedOk) {
            registerAuthSuccess(username);
        }

        return loggedOk;
    }

    private void init() {
        // Configure the list of failed users to delete the oldest entry once it
        // exceeds the specified size
        failedUsers = new LinkedHashMap<String, LockRecord>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<String, LockRecord> eldest) {
                if (size() > cacheSize) {
                    // Check to see if this element has been removed too quickly
                    long timeInCache = (TimeUtil.getCurrentTime() -
                            eldest.getValue().getLastFailureTime()) / 1000;

                    if (timeInCache < cacheRemovalWarningTime) {
                        ConsoleUtil.warn("User [" + eldest.getKey() + "] was removed from the failed users cache after [" + timeInCache + "] seconds to keep the cache size within the limit set");
                    }
                    return true;
                }
                return false;
            }
        };
    }

    public boolean isLocked(String username) {
        LockRecord lockRecord;
        synchronized (this) {
            lockRecord = failedUsers.get(username);
        }

        // No lock record means user can't be locked
        if (lockRecord == null) {
            return false;
        }

        // Check to see if user is locked. Or: User has not, yet, exceeded lock thresholds
        return lockRecord.getFailures() >= getFailureCount() &&
                (TimeUtil.getCurrentTime() -
                        lockRecord.getLastFailureTime()) / 1000 < getLockOutTime();
    }

    /*
     * After a failed authentication, add the record of the failed
     * authentication.
     */
    private void registerAuthFailure(String username) {
        LockRecord lockRecord;
        synchronized (this) {
            if (!failedUsers.containsKey(username)) {
                lockRecord = new LockRecord();
                failedUsers.put(username, lockRecord);
            } else {
                lockRecord = failedUsers.get(username);
                if (lockRecord.getFailures() >= getFailureCount() &&
                        ((TimeUtil.getCurrentTime() - lockRecord.getLastFailureTime()) / 1000)
                                > getLockOutTime()) {
                    // User was previously locked out but lockout has now
                    // expired so reset failure count
                    lockRecord.resetFailures();
                }
            }
        }
        lockRecord.registerFailure();
    }

    /*
     * After successful authentication, any record of previous authentication
     * failure is removed.
     */
    private synchronized void registerAuthSuccess(String username) {
        // Successful authentication means removal from the list of failed users
        failedUsers.remove(username);
    }

    public static class LockRecord {
        private final AtomicInteger failures = new AtomicInteger(0);

        private long lastFailureTime = 0;

        public int getFailures() {
            return failures.get();
        }

        void resetFailures() {
            failures.set(0);
        }

        public long getLastFailureTime() {
            return lastFailureTime;
        }

        void registerFailure() {
            failures.incrementAndGet();
            lastFailureTime = TimeUtil.getCurrentTime();
        }
    }
}
