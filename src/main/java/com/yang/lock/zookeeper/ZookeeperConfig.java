package com.yang.lock.zookeeper;

import com.yang.lock.LockConfig;
import org.apache.zookeeper.Watcher;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public interface ZookeeperConfig {
    /**
     * zookeeper的连接字符串
     *
     * @return 连接字符串
     */
    String connectStr();


    int sessionTimeout();

    Watcher watcher();

    LockConfig lockConfig();

}
