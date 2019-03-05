package com.yang.test;

import com.yang.lock.DistributedLock;
import com.yang.lock.LockConfig;
import com.yang.lock.zookeeper.LockUtil;

import java.util.concurrent.TimeUnit;

/**
 * 描述:
 * 作者:杨川东
 * 日期:2019-03-05
 */
public class LockTest {

    public static void main(String[] args) {
        final DistributedLock lock = LockUtil.newZookeeperLock("127.0.0.1:2181", new LockConfig() {
            @Override
            public int timeOut() {
                //超时时间是毫秒
                return 30000;
            }

            @Override
            public String lockName() {
                return "testlock";
            }
        });

        for (int i = 0; i < 3; i++) {
            final int index = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    System.out.println(Thread.currentThread() + "====is run");
                    try {
                        TimeUnit.SECONDS.sleep(index + 1);
                        System.out.println(Thread.currentThread() + "====is stop");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }).start();
        }

    }
}
