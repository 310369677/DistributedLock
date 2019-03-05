package com.yang.lock.zookeeper;

import com.yang.lock.exceptions.InternalRuntimeException;
import com.yang.lock.exceptions.NetWorkException;
import com.yang.lock.util.SimpleUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-21
 */
class ZookeeperLockSupporter {

    private String basePath;

    private String path;

    private String lockName;

    private ZooKeeper zooKeeper;

    private String nodePath;

    private boolean lock = false;


    //等待释放锁被挂起的时间设置为1分钟，1分钟后重新尝试获取锁
    private static final long AWAIT_TIME_OUT_MINS = 1;


    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperLockSupporter.class);

    ZookeeperLockSupporter(ZooKeeper zooKeeper, String basePath, String lockName) {
        this.basePath = basePath;
        this.lockName = lockName;
        this.zooKeeper = zooKeeper;
        this.path = SimpleUtils.pathJoin(basePath, lockName);
    }

    /**
     * 请求路劲
     *
     * @return 路劲的值
     */
    protected String acquirePath() {
        //尽最大可能的去创建临时锁节点
        nodePath = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<String>() {
            @Override
            public String run() throws KeeperException, InterruptedException {
                return createNewLockNode();
            }
        });
        return nodePath;
    }

    private List<String> getAllSortedLockNode() {
        List<String> children = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<List<String>>() {

            @Override
            public List<String> run() throws Exception {
                return zooKeeper.getChildren(basePath, null);
            }
        });
        if (children == null) {
            return null;
        }
        Collections.sort(children, new Comparator<String>() {
            @Override
            public int compare(String node1, String node2) {
                return (int) (splitNodeIndex(node1) - splitNodeIndex(node2));
            }
        });
        return children;
    }

    private long splitNodeIndex(String currentThreadLockNodePath) {
        int index = currentThreadLockNodePath.lastIndexOf(lockName);
        return Long.parseLong(currentThreadLockNodePath.substring(index + lockName.length()));
    }

    private String deletePre(String source, String pre) {
        if (source == null || "".equals(source) || pre == null) {
            return "";
        }
        int index = source.indexOf(pre);
        return source.substring(index + pre.length());
    }

    /**
     * 创建新的临时节点
     *
     * @return 新的临时节点的路劲
     * @throws KeeperException      异常
     * @throws InterruptedException 异常
     */
    private String createNewLockNode() throws KeeperException, InterruptedException {
        //创建节点并放入当前创建的时间数据
        return zooKeeper.create(path, SimpleUtils.getBytes(System.currentTimeMillis()), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }


    public boolean isLock() {
        return lock;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void waitLock() {
        if (isLock()) {
            return;
        }
        if (getNodePath() == null) {
            throw new InternalRuntimeException("nodePath 不能为空");
        }
        while (!isLock()) {
            //先判断下当前nodePath是否存在，当前的nodePath可能因为网络断后，重新连接上，当前线程锁被清理掉了
            Stat stat = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<Stat>() {
                @Override
                public Stat run() throws Exception {
                    return zooKeeper.exists(nodePath, null);
                }
            });
            if (stat == null && (acquirePath() == null)) {
                throw new NetWorkException("网络错误，请求创建新的节点失败");
            }
            //得到当前节点的索引
            final int index = (int) splitNodeIndex(nodePath);
            //得到所有排序好后注册的节点
            List<String> list = getAllSortedLockNode();
            if (list == null) {
                throw new NetWorkException("获取所有孩子节点网络错误");
            }
            //获得锁
            if (index == splitNodeIndex(list.get(0))) {
                lock = true;
                return;
            }
            int count = list.indexOf(deletePre(nodePath, basePath + "/"));
            //等待锁的释放
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            //得到比这个节点序列小1的上个节点
            final ZookeeperWatcher zookeeperWatcher = new ZookeeperWatcher(zooKeeper);
            zookeeperWatcher.addListener(Watcher.Event.EventType.NodeDeleted, new ZookeeperWatcher.Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    LOGGER.debug("接收到节点{}删除事件", event.getPath());
                    countDownLatch.countDown();
                }
            });
            //得到上一个节点
            final String preNodePath = list.get(count - 1);

            Stat stat1 = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<Stat>() {
                @Override
                public Stat run() throws Exception {
                    return zooKeeper.exists(getNodeFullPath(preNodePath), zookeeperWatcher);
                }
            });
            //上一个节点不存在,可能上个节点的锁已经释放,重新去获得锁
            if (stat1 == null) {
                continue;
            }
            try {
                if (countDownLatch.await(AWAIT_TIME_OUT_MINS, TimeUnit.MINUTES)) {
                    //锁被获得
                    lock = true;
                    return;
                }
            } catch (InterruptedException e) {
                LOGGER.error("当前线程被打断", e);
                Thread.currentThread().interrupt();
            }
        }
    }


    public void releaseLock() {
        if (nodePath == null || !lock) {
            return;
        }
        SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<Void>() {
            @Override
            public Void run() throws KeeperException, InterruptedException {
                LOGGER.debug("删除当前的节点{}", nodePath);
                zooKeeper.delete(nodePath, -1);
                return null;
            }
        });
        nodePath = null;
        lock = false;
    }

    private static class ZookeeperWatcher implements Watcher {

        EnumMap<Event.EventType, Watcher> enumMap;
        ZooKeeper zooKeeper;

        ZookeeperWatcher(ZooKeeper zooKeeper) {
            this.zooKeeper = zooKeeper;
            enumMap = new EnumMap<>(Event.EventType.class);
        }

        void addListener(Event.EventType eventType, Watcher watcher) {
            if (eventType == null || watcher == null) {
                return;
            }
            enumMap.put(eventType, watcher);
        }

        public interface Watcher {
            void process(WatchedEvent event);
        }

        @Override
        public void process(WatchedEvent event) {
            Event.EventType eventType = event.getType();
            if (enumMap.containsKey(eventType)) {
                enumMap.get(eventType).process(event);
            }
            //继续注册
            zooKeeper.register(this);
        }
    }

    private String getNodeFullPath(String nodePath) {
        return basePath + "/" + nodePath;
    }


}
