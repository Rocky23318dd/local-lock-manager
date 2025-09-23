package com.rocky.wlock.newlock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.rocky.wlock.constants.ServiceConstants.LOCK_DEFAULT_CHECK_INTERVAL_SECONDS;
import static com.rocky.wlock.constants.ServiceConstants.LOCK_DEFAULT_KEY_EXPIRE_TIME_SECONDS;

@Slf4j
public class LocalLockManager implements LockManager {
    private final Cache<String, ResettableLock> cache;
    private final HashedWheelTimer wheelTimer;
    private final Map<String, Timeout> timerTaskMap;
    private final long lockCheckInterval;
    private final long keyRenewInterval;
    private final TimeUnit unit;

    public LocalLockManager() {
        this(LOCK_DEFAULT_CHECK_INTERVAL_SECONDS, LOCK_DEFAULT_KEY_EXPIRE_TIME_SECONDS, TimeUnit.SECONDS);
    }

    public LocalLockManager(long lockCheckInterval, TimeUnit unit) {
        this(lockCheckInterval, LOCK_DEFAULT_KEY_EXPIRE_TIME_SECONDS, unit);
    }

    public LocalLockManager(long lockCheckInterval, long keyExpireTime, TimeUnit unit) {
        this.lockCheckInterval = lockCheckInterval;
        this.keyRenewInterval = (keyExpireTime / 3) > 0 ? keyExpireTime / 3 : 1;
        this.unit = unit;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(keyExpireTime, unit).build();
        this.wheelTimer = new HashedWheelTimer(new DefaultThreadFactory("watch-timer", true));
        this.timerTaskMap = new ConcurrentHashMap<>();
    }

    @Override
    public void acquire(String key) {
        if(cache.getIfPresent(key) == null){
            createLockIfNotExist(key);
            acquire(key);
        }else{
            ResettableLock resettableLock = cache.getIfPresent(key);
            resettableLock.lock();
            if (resettableLock.getStatus() == 1) {
                if (!timerTaskMap.containsKey(key)) {
                    refreshKey(key);
                }
                checkLock(resettableLock, Thread.currentThread());
            }
        }
    }

    private void createLockIfNotExist(String key) {
        if (cache.getIfPresent(key) == null) {
            cache.asMap().computeIfAbsent(key, (k) -> {
                if (cache.getIfPresent(key) != null) {
                    return cache.getIfPresent(key);
                }
                return new ResettableLock();
            });
        }
    }

    @Override
    public void release(String key) {
        ResettableLock lock = cache.getIfPresent(key);
        if (lock == null) {
            return;
        }
        lock.unlock();
    }

    @Override
    public boolean tryAcquire(String key, long timeout, TimeUnit timeUnit) throws InterruptedException {
        if(cache.getIfPresent(key) == null){
            createLockIfNotExist(key);
            return tryAcquire(key,timeout,timeUnit);
        }else{
            ResettableLock resettableLock = cache.getIfPresent(key);
            boolean result = resettableLock.tryLock(timeout,timeUnit);
            if (result) {
                if (!timerTaskMap.containsKey(key)) {
                    refreshKey(key);
                }
                checkLock(resettableLock, Thread.currentThread());
            }
            return result;
        }
    }

    private void checkLock(ResettableLock lock, Thread targetThread) {
        wheelTimer.newTimeout(timeout -> {
            if (lock.getHolderThread() == targetThread) {
                if (targetThread.isAlive()) {
                    checkLock(lock, targetThread);
                } else {
                    lock.reset(targetThread);
                }
            }

        }, lockCheckInterval, unit);
    }

    private Timeout renewKey(String key) {
        return wheelTimer.newTimeout(timeout -> {
            ResettableLock lock = cache.getIfPresent(key);
            if (!timeout.isCancelled() && lock != null && (lock.isHolderThreadActive() || lock.hasWaitingThreads())) {
                timerTaskMap.get(key).cancel();
                timerTaskMap.remove(key);
                refreshKey(key);
            } else {
                timerTaskMap.remove(key);
            }
        }, keyRenewInterval, unit);
    }

    private void refreshKey(String key) {
        cache.asMap().computeIfPresent(key, (k, lock) -> lock);
        timerTaskMap.put(key, renewKey(key));
    }

    public void printInfo() {
        System.out.println("key count " + cache.asMap().size());
        //@TODO timer中的数据去除
        System.out.println("time task count " + timerTaskMap.size());
    }
}
