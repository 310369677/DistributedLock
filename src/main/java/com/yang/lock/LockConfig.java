package com.yang.lock;

/**
 * 描述:锁的配置
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-4-2
 */
public interface LockConfig {

    /**
     * 锁超时，代表这把锁最多能被持有的时间，而不被释放,-1代表永远不会被动释放，只能主动释放
     *
     * @return 锁多少时间后超时，单位是毫秒,超时后，这把锁可以被其他竞争者获得
     */
    int timeOut();

    /**
     * 锁的名字，必传
     *
     * @return 锁的名字
     */
    String lockName();
}
