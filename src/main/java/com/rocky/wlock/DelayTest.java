package com.rocky.wlock;

import com.rocky.wlock.newlock.LocalLockManager;

public class DelayTest {
    public static void main(String[] args) throws InterruptedException {
        LocalLockManager manager = new LocalLockManager();
        manager.tryAcquire("test",100000,null);
        manager.tryAcquire("test",100000,null);

        manager.tryAcquire("test",100000,null);

        manager.tryAcquire("test",100000,null);

        manager.tryAcquire("test",100000,null);

        manager.tryAcquire("test",100000,null);

    }
}
