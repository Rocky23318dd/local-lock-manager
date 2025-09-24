package com.rocky.wlock.test;

import com.rocky.wlock.lock.LocalLockManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 对比测试
 */
public class CompareTest {
    public static void main(String[] args) throws InterruptedException {
        LocalLockManager lockManager = new LocalLockManager();
        ReentrantLock lock = new ReentrantLock();
        int[] values = new int[1];
        long startTimeMillis = System.currentTimeMillis();
        int testTime = 100000;
        CountDownLatch singleBucketCT = new CountDownLatch(testTime);
        CountDownLatch multipleBucketCT = new CountDownLatch(testTime);
        CountDownLatch ct2 = new CountDownLatch(testTime);

        System.out.println("【单key并发测试】");
        for(int i = 0; i < testTime; i++){
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
        System.out.println("time:" + (endTimeMills - startTimeMillis)/1000 + "." + (endTimeMills - startTimeMillis)%1000+"s");


        System.out.println("【多Key并发测试】");
        int[] multiValues = new int[1000];
        startTimeMillis = System.currentTimeMillis();
        for(int i = 0; i < testTime; i++){
            new Thread(() -> {
                int val = (int) (Math.random()*100);
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
        endTimeMills = System.currentTimeMillis();
        int result = 0;
        for(int num : multiValues){
            result += num;
        }
        System.out.println("value:" + result);
        System.out.println("time:" + (endTimeMills - startTimeMillis)/1000 + "." + (endTimeMills - startTimeMillis)%1000+"s");


        System.out.println("【单键ReentrantLock测试】");
        values[0] = 0;
        startTimeMillis = System.currentTimeMillis();
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
        endTimeMills = System.currentTimeMillis();
        System.out.println("value:" + values[0]);
        System.out.println("time:" + (endTimeMills - startTimeMillis)/1000 + "." + (endTimeMills - startTimeMillis)%1000+"s");
    }
}
