package com.rocky.wlock.lock;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class ResettableLock {

    private final NonFairSync innerSync;

    /**
     * 加锁入口方法
     */
    public void lock() {
        if (!innerSync.tryAcquireImmediately()) {
            innerSync.acquire(1);
        }
    }

    /**
     * 解锁入口方法
     */
    public void unlock() {
        if (Thread.currentThread() != innerSync.getHolderThread() || innerSync.getStatus() == 0) {
            throw new IllegalMonitorStateException();
        }
        innerSync.release(1);
    }

    /**
     * 超时等待获取
     */
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return innerSync.tryAcquireImmediately() || innerSync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
    /**
     * 外部可调用该方法强制释放锁
     *
     * @param target 目标线程
     */
    public void reset(Thread target) {
        if (innerSync.getHolderThread() == target) {
            innerSync.release(innerSync.getStatus());
        }
    }

    /**
     * 获取持有锁的持有线程
     */
    public Thread getHolderThread() {
        return innerSync.getHolderThread();
    }

    /**
     * 判断持有锁的线程是否存活
     */
    public boolean isHolderThreadActive() {
        return innerSync.getHolderThread() != null && innerSync.getHolderThread().isAlive();
    }

    /**
     * 判断是否有正在等待锁的线程
     */
    public boolean hasWaitingThreads() {
        return innerSync.hasQueuedThreads();
    }

    /**
     * 获取锁状态信息
     */
    public int getStatus(){
        return innerSync.getStatus();
    }

    public ResettableLock() {
        innerSync = new NonFairSync();
    }

    private static class NonFairSync extends AbstractQueuedSynchronizer {
        /**
         * 竞争锁的核心逻辑，必须重写
         *
         * @param arg 外部传入参数
         */
        @Override
        protected boolean tryAcquire(int arg) {
            if (getState() == 0 && getExclusiveOwnerThread() == null && compareAndSetState(0, arg)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * 在入队竞争前尝试获取锁(锁重入逻辑）
         */
        private boolean tryAcquireImmediately() {
            int state = getState();
            // 如果无人持有锁，则尝试获取锁
            if (state == 0 && getExclusiveOwnerThread() == null && compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
                // 如果有人持有锁，则判断当前线程是否为持有锁的线程
            } else if (isHeldExclusively()) {
                setState(state + 1);
                return true;
            }
            return false;
        }

        /**
         * 释放锁的核心逻辑，必须重写
         */
        @Override
        public boolean tryRelease(int arg) {
            if (getState() > 0) {
                if (compareAndSetState(getState(), getState() - arg) && getState() == 0) {
                    setExclusiveOwnerThread(null);
                    return true;
                }
            }
            return false;
        }

        private Thread getHolderThread() {
            return getExclusiveOwnerThread();
        }

        private int getStatus() {
            return super.getState();
        }

        /**
         * 是否被独占，必须被重写
         */
        @Override
        public boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }
}
