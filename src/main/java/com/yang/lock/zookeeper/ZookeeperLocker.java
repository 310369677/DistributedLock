package com.yang.lock.zookeeper;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-4-2
 */
class ZookeeperLocker implements Serializable {


    private static final long serialVersionUID = -5160909604718537053L;

    /**
     * 锁的名字
     */
    private String lockerName;


    /**
     * 锁的创建时间
     */
    private long createTime;

    /**
     * 锁的最后修改时间
     */
    private long lastUpdateTime;


    /**
     * 超时时间
     */
    private long timeOut;

    private AtomicInteger version;

    private ZookeeperLocker(String lockerName, long createTime, long lastUpdateTime, long timeOut) {
        this.lockerName = lockerName;
        this.createTime = createTime;
        this.lastUpdateTime = lastUpdateTime;
        this.timeOut = timeOut;
        version = new AtomicInteger(0);
    }

    public String getLockerName() {
        return lockerName;
    }

    public void setLockerName(String lockerName) {
        this.lockerName = lockerName;
    }


    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public static ZookeeperLocker newInstance(String lockerName, long timeOut) {
        return new ZookeeperLocker(lockerName, System.currentTimeMillis(), System.currentTimeMillis(), timeOut);
    }

    public AtomicInteger getVersion() {
        return version;
    }

    public ZookeeperLocker resetVersion(int version) {
        this.version = new AtomicInteger(version);
        return this;
    }

    public long getTimeOut() {
        return timeOut;
    }
}
