package com.yang.lock.zookeeper;

import com.yang.lock.LockConfig;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public class ZookeeperConfigAdapter implements ZookeeperConfig {


    private String connectStr;
    private int sessionTimeOut = 10000;
    private Watcher watcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {

        }
    };
    private LockConfig lockConfig = new LockConfig() {
        @Override
        public int timeOut() {
            return sessionTimeOut;
        }

        @Override
        public String lockName() {
            return null;
        }
    };

    public ZookeeperConfigAdapter(String connectStr, int sessionTimeOut, Watcher watcher, LockConfig lockConfig) {
        this.connectStr = connectStr;
        if (sessionTimeOut > 0) {
            this.sessionTimeOut = sessionTimeOut;
        }
        if (watcher != null) {
            this.watcher = watcher;
        }
        if (lockConfig != null) {
            this.lockConfig = lockConfig;
        }
    }

    @Override
    public String connectStr() {
        return connectStr;
    }

    /**
     * 默认10s超时
     *
     * @return session超时的时间，默认是10s
     */
    @Override
    public int sessionTimeout() {
        return sessionTimeOut;
    }

    /**
     * 默认不处理
     *
     * @return 观察者
     */
    @Override
    public Watcher watcher() {
        return watcher;
    }

    @Override
    public LockConfig lockConfig() {
        return lockConfig;
    }


}
