package com.yang.lock;

/**
 * 描述:分布式锁接口
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public interface DistributedLock {

    /**
     * 得到锁
     */
    void lock();

    /**
     * 释放锁
     */
    void unlock();
    
}
