package com.yang.lock.zookeeper;

import com.yang.lock.DistributedLock;
import com.yang.lock.LockConfig;
import com.yang.lock.exceptions.CreateLockerException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public final class LockUtil {

    //最大支持创建锁的数量
    public static final int MAX_LOCK_NUM = 100;


    private LockUtil() {

    }

    public static DistributedLock newZookeeperLock(ZookeeperConfig zookeeperConfig) {
        if (zookeeperConfig == null) {
            throw new CreateLockerException("ZookeeperConfig不能为null");
        }
        if (zookeeperConfig.lockConfig() == null) {
            throw new CreateLockerException("LockConfig不能为null");
        }
        if (zookeeperConfig.lockConfig().lockName() == null || "".equals(zookeeperConfig.lockConfig().lockName())) {
            throw new CreateLockerException("lockname不能为空");
        }
        if (zookeeperConfig.lockConfig().timeOut() <= 0) {
            throw new CreateLockerException("lock的超时时间不能为负数");
        }
        return ZookeeperDistributedLock.newZookeeperDistributedLock(zookeeperConfig, MAX_LOCK_NUM);
    }

    public static DistributedLock newZookeeperLock(String connectStr, LockConfig lockConfig) {
        ZookeeperConfigAdapter zookeeperConfigAdapter = new ZookeeperConfigAdapter(connectStr, 30000, null, lockConfig);
        return newZookeeperLock(zookeeperConfigAdapter);
    }

    public static void main(String[] args) throws InterruptedException {
        final DistributedLock lock=LockUtil.newZookeeperLock("127.0.0.1:2181", new LockConfig() {
            @Override
            public int timeOut() {
                return 30000;
            }

            @Override
            public String lockName() {
                return "testLock112wq";
            }
        });
        final DistributedLock lock1=LockUtil.newZookeeperLock("127.0.0.1:2181", new LockConfig() {
            @Override
            public int timeOut() {
                return 30000;
            }

            @Override
            public String lockName() {
                return "testLocj12";
            }
        });
        final CountDownLatch countDownLatch=new CountDownLatch(3);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    System.out.println("线程1得到锁");
                    TimeUnit.SECONDS.sleep(2);
                    System.out.println("线程1释放锁");
                    lock.unlock();
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println("线程2得多锁");
                    TimeUnit.SECONDS.sleep(3);
                    System.out.println("线程2释放锁");
                    lock.unlock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock1.lock();
                System.out.println("线程3另外一把锁得到");
                try {
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println("线程3释放了...");
                    lock1.unlock();
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        countDownLatch.await();

    }
}
