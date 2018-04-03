package com.yang.lock.zookeeper;

import com.yang.lock.DistributedLock;
import com.yang.lock.LockConfig;
import com.yang.lock.exceptions.CreateLockerException;

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
}
