package com.rocky.wlock.test;

import com.rocky.wlock.lock.LocalLockManager;

import java.util.concurrent.CountDownLatch;

/**
 * LocalLockManager 多key测试
 */
public class MultipleKeyPerformTest {
    public static void main(String[] args) throws InterruptedException {
        LocalLockManager lockManager = new LocalLockManager();
        int testTime = 10000;
        CountDownLatch multipleBucketCT = new CountDownLatch(testTime);
        System.out.println("【多Key并发测试】");
        int[] multiValues = new int[10001];
        long startTimeMillis = System.currentTimeMillis();
        for(int i = 0; i < testTime; i++){
            new Thread(() -> {
                int val = (int) (Math.random()*10000);
                String key = String.valueOf( val);
                lockManager.acquire(key);
                lockManager.acquire(key);
                multiValues[val]++;
                lockManager.release(key);
                lockManager.release(key);
                multipleBucketCT.countDown();
            }).start();
        }
        multipleBucketCT.await();
        long endTimeMills = System.currentTimeMillis();
        int result = 0;
        for(int num : multiValues){
            result += num;
        }
        System.out.println("value:" + result);
        System.out.println("time:" + (endTimeMills - startTimeMillis)/1000 + "." + (endTimeMills - startTimeMillis)%1000+"s");
        Thread.sleep(8000);
        lockManager.printInfo();
    }
}
