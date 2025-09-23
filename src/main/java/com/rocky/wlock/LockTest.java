package com.rocky.wlock;

import com.rocky.wlock.newlock.LocalLockManager;

import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LockTest {
    public static void main(String[] args) throws InterruptedException {
        LocalLockManager manager = new LocalLockManager();
        final int[] values = new int[501];
        int[] exCount = new int[1001];
        AtomicInteger timeoutCount = new AtomicInteger(0);
        //????
        int threadCount = 100;
        int keyScale = 10;
        double delayRate = 0.01;
        double tryLockRate = 1;
        double maxTryDelayTime = 200;
        long baseSleepTime = 4000;
        long maxDeltaSleepTime = 1000;//unit:milliseconds.
        double exceptionRate = 0.00;

        CountDownLatch ct = new CountDownLatch(threadCount);
        SecureRandom secureRandom = new SecureRandom();
        long current = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                String key = String.valueOf(secureRandom.nextInt(keyScale));
                if(Math.random()<tryLockRate){
                    try {
                        if(manager.tryAcquire(key.intern(), 2000, TimeUnit.MILLISECONDS)){
                            manager.acquire(key.intern());
                        }else{
                            timeoutCount.incrementAndGet();
                            ct.countDown();
                            return;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                manager.acquire(key.intern());
                manager.acquire(key.intern());
                if(Math.random()<exceptionRate){
                    ct.countDown();
                    exCount[Integer.parseInt(key)]++;
                    throw new RuntimeException();
                }
                values[Integer.parseInt(key)]++;
                manager.release(key);
                manager.release(key);
                ct.countDown();
            }).start();
        }
        ct.await();
        long end = System.currentTimeMillis();
        Thread.sleep(1000);
        int result = 0;
        int resultExCount = 0;
        for (int num : values) {
            result += num;
        }
        for (int num : exCount) {
            resultExCount += num;
        }
        System.out.println("result:" + result + " exCount:" + resultExCount +" timeountCount:"+timeoutCount);
        System.out.println("time cost: " + (end - current)+"ms");
        manager.printInfo();
    }
}
