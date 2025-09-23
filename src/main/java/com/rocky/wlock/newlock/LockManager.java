package com.rocky.wlock.newlock;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public interface LockManager {
    void acquire (String key) throws ExecutionException;
    void release (String key);
    boolean tryAcquire(String key,long timeOut, TimeUnit timeUnit) throws InterruptedException;
}
