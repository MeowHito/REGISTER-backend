package com.actionth.membership.service.impl;

import com.actionth.membership.exception.LockAcquisitionException;
import com.actionth.membership.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Implementation of DistributedLockService using Redisson (Redis-based distributed locks).
 * 
 * Benefits:
 * - Works across multiple application instances
 * - Automatic lock release on crash (via lease time)
 * - Much faster than database-level locking
 * - Supports fairness and reentrant locks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_TIME = 5; // seconds
    private static final long DEFAULT_LEASE_TIME = 30; // seconds
    private static final String LOCK_PREFIX = "quota:lock:";

    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            
            if (!acquired) {
                log.warn("Failed to acquire lock: {} after waiting {} {}", fullLockKey, waitTime, unit);
                throw new LockAcquisitionException(lockKey, unit.toMillis(waitTime));
            }
            
            log.debug("Lock acquired: {}", fullLockKey);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, unit.toMillis(waitTime), e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", fullLockKey);
            }
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, task);
    }

    @Override
    public <T> T executeWithMultiLock(List<String> lockKeys, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            return task.get();
        }

        // Sort keys to prevent deadlock (always acquire in same order)
        List<String> sortedKeys = lockKeys.stream()
                .distinct()
                .sorted()
                .toList();

        // Create RLock array for multi-lock
        RLock[] locks = sortedKeys.stream()
                .map(key -> redissonClient.getLock(LOCK_PREFIX + key))
                .toArray(RLock[]::new);

        // Use RedissonMultiLock for atomic multi-lock acquisition
        RLock multiLock = redissonClient.getMultiLock(locks);

        boolean acquired = false;
        try {
            acquired = multiLock.tryLock(waitTime, leaseTime, unit);

            if (!acquired) {
                log.warn("Failed to acquire multi-lock for keys: {} after waiting {} {}", sortedKeys, waitTime, unit);
                throw new LockAcquisitionException(String.join(",", sortedKeys), unit.toMillis(waitTime));
            }

            log.debug("Multi-lock acquired for keys: {}", sortedKeys);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(String.join(",", sortedKeys), unit.toMillis(waitTime), e);
        } finally {
            if (acquired) {
                multiLock.unlock();
                log.debug("Multi-lock released for keys: {}", sortedKeys);
            }
        }
    }

    @Override
    public long incrementAndGet(String counterKey) {
        String fullKey = "counter:" + counterKey;
        return redissonClient.getAtomicLong(fullKey).incrementAndGet();
    }

    @Override
    public void setCounterExpiry(String counterKey, long ttl, TimeUnit unit) {
        String fullKey = "counter:" + counterKey;
        redissonClient.getAtomicLong(fullKey).expire(java.time.Duration.ofMillis(unit.toMillis(ttl)));
    }
}
