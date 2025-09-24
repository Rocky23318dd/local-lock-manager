package com.rocky.wlock.test;

import com.rocky.wlock.lock.LocalLockManager;

import java.util.concurrent.CountDownLatch;

/**
 * LocalLockManager单key测试
 */
public class SingleKeyPerformTest {
    public static void main(String[] args) throws InterruptedException {
        LocalLockManager lockManager = new LocalLockManager();
        int[] values = new int[1];
        long startTimeMillis = System.currentTimeMillis();
        int testTime = 100000;
        CountDownLatch singleBucketCT = new CountDownLatch(testTime);

        System.out.println("【单key并发测试】");
        for (int i = 0; i < testTime; i++) {
            new Thread(() -> {
                lockManager.acquire("1");
                lockManager.acquire("1");
                values[0]++;
                lockManager.release("1");
                lockManager.release("1");
                singleBucketCT.countDown();
            }).start();
        }
        singleBucketCT.await();
        long endTimeMills = System.currentTimeMillis();
        System.out.println("value:" + values[0]);
        System.out.println("time:" + (endTimeMills - startTimeMillis) / 1000 + "." + (endTimeMills - startTimeMillis) % 1000 + "s");
    }

}
