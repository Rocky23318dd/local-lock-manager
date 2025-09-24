package com.rocky.wlock.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  ReentrantLock 单key测试
 */
public class SingleReentrantLockPerformTest {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        int[] values = new int[1];
        int testTime = 100000;
        CountDownLatch ct2 = new CountDownLatch(testTime);
        System.out.println("【单键ReentrantLock测试】");
        long startTimeMillis = System.currentTimeMillis();
        for(int i = 0; i < testTime; i++){
            new Thread(() -> {
                lock.lock();
                lock.lock();
                values[0]++;
                lock.unlock();
                lock.unlock();
                ct2.countDown();
            }).start();
        }
        ct2.await();
        long endTimeMills = System.currentTimeMillis();
        System.out.println("value:" + values[0]);
        System.out.println("time:" + (endTimeMills - startTimeMillis)/1000 + "." + (endTimeMills - startTimeMillis)%1000+"s");
        System.gc();
    }
}
