package com.actionth.membership.exception;

/**
 * Exception thrown when a distributed lock cannot be acquired within the timeout period.
 * This typically occurs during high traffic when resources are contended.
 */
public class LockAcquisitionException extends RuntimeException {

    private final String lockKey;
    private final long waitTimeMs;

    public LockAcquisitionException(String lockKey, long waitTimeMs) {
        super(String.format("Failed to acquire lock '%s' within %d ms. Please try again.", lockKey, waitTimeMs));
        this.lockKey = lockKey;
        this.waitTimeMs = waitTimeMs;
    }

    public LockAcquisitionException(String lockKey, long waitTimeMs, Throwable cause) {
        super(String.format("Failed to acquire lock '%s' within %d ms. Please try again.", lockKey, waitTimeMs), cause);
        this.lockKey = lockKey;
        this.waitTimeMs = waitTimeMs;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getWaitTimeMs() {
        return waitTimeMs;
    }
}
