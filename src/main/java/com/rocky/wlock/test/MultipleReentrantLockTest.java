package com.rocky.wlock.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ConcurrentHashMap + ReentrantLock 模拟多key测试
 */
public class MultipleReentrantLockTest {
    public static void main(String[] args) throws InterruptedException {
        int testTime = 10000;
        CountDownLatch multipleBucketCT = new CountDownLatch(testTime);
        System.out.println("【ReentrantLock多Key并发测试】");
        ConcurrentHashMap<String, ReentrantLock> map = new ConcurrentHashMap<>();
        int[] multiValues = new int[10001];
        long startTimeMillis = System.currentTimeMillis();
        for(int i = 0; i < testTime; i++){
            new Thread(() -> {
                int val = (int) (Math.random()*10000);
                String key = String.valueOf( val);
                map.computeIfAbsent(key, (k) -> new ReentrantLock());
                ReentrantLock lock = map.get(key);
                lock.lock();
                lock.lock();
                multiValues[val]++;
                lock.unlock();
                lock.unlock();
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
    }
}

