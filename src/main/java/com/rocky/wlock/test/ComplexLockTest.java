package com.rocky.wlock.test;

import com.rocky.wlock.lock.LocalLockManager;

import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LocalLockManager 复合条件测试
 */
public class ComplexLockTest {
    public static void main(String[] args) throws InterruptedException {
        LocalLockManager manager = new LocalLockManager();
        final int[] values = new int[501];
        int[] exCount = new int[1001];
        AtomicInteger timeoutCount = new AtomicInteger(0);
        //测试参数
        int threadCount = 10000;//线程数
        int keyScale = 100;//key规模范围
        double sleepRate = 0.10;//睡眠概率（测试renewKey和checkLock）
        double tryLockRate = 0.2;//定时等待tryAcquire触发概率
        long maxWaitTime = 200;//定时等待最大时间unit:milliseconds.
        long baseSleepTime = 4000;//基础睡眠时间unit:milliseconds.
        long maxDeltaSleepTime = 1000;//最大睡眠时间差值unit:milliseconds.
        double exceptionRate = 0.2;//异常概率（测试key自动过期）

        CountDownLatch ct = new CountDownLatch(threadCount);
        SecureRandom secureRandom = new SecureRandom();
        long current = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                String key = String.valueOf(secureRandom.nextInt(keyScale));
                if(Math.random()<tryLockRate){
                    try {
                        if(manager.tryAcquire(key.intern(), (long) (Math.random()*maxWaitTime), TimeUnit.MICROSECONDS)){
                            manager.acquire(key.intern());
                        }else{
                            timeoutCount.incrementAndGet();
                            ct.countDown();
                            return;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    manager.acquire(key.intern());
                    manager.acquire(key.intern());
                }
                if(Math.random()<sleepRate){
                    try {
                        Thread.sleep(baseSleepTime + secureRandom.nextInt((int) maxDeltaSleepTime));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
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
        Thread.sleep(100);
        System.out.println("result:" + result + " exCount:" + resultExCount +" timeountCount:"+timeoutCount);
        System.out.println("time cost: " + (end - current)+"ms");
        manager.printInfo();
    }
}
