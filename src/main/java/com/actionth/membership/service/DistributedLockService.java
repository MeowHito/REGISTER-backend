package com.actionth.membership.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for distributed locking to handle race conditions
 * when multiple instances or concurrent requests access shared resources.
 */
public interface DistributedLockService {

    /**
     * Execute a task with a distributed lock.
     * 
     * @param lockKey The unique key for the lock (e.g., "quota:pricing:uuid-123")
     * @param waitTime Maximum time to wait for acquiring the lock
     * @param leaseTime Maximum time to hold the lock (auto-release after this)
     * @param unit Time unit for waitTime and leaseTime
     * @param task The task to execute while holding the lock
     * @return The result of the task
     * @throws LockAcquisitionException if lock cannot be acquired within waitTime
     */
    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task);

    /**
     * Execute a task with a distributed lock using default timeout (5s wait, 30s lease).
     */
    <T> T executeWithLock(String lockKey, Supplier<T> task);

    /**
     * Execute a task with multiple locks (for ordering multiple items).
     * Locks are acquired in sorted order to prevent deadlocks.
     */
    <T> T executeWithMultiLock(java.util.List<String> lockKeys, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task);

    /**
     * Atomically increment a counter and return the new value.
     * This is useful for generating unique sequential numbers (e.g., order numbers).
     * 
     * @param counterKey The unique key for the counter
     * @return The incremented value
     */
    long incrementAndGet(String counterKey);

    /**
     * Set expiration time for a counter key.
     * Useful for daily counters that should reset.
     * 
     * @param counterKey The unique key for the counter
     * @param ttl Time to live
     * @param unit Time unit
     */
    void setCounterExpiry(String counterKey, long ttl, TimeUnit unit);
}
