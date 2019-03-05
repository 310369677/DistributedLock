package com.yang.lock.zookeeper;

import com.yang.lock.DistributedLock;
import com.yang.lock.exceptions.CreateLockerException;
import com.yang.lock.exceptions.CreateZookeeperException;
import com.yang.lock.exceptions.LockInValiteException;
import com.yang.lock.exceptions.NetWorkException;
import com.yang.lock.util.ObjectSerializeUtil;
import com.yang.lock.util.SimpleUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 描述:zookeeper
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
class ZookeeperDistributedLock implements DistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperDistributedLock.class);

    //zookeeper的配置文件
    private ZookeeperConfig zookeeperConfig;

    //zookeeper实例
    private ZooKeeper zooKeeper;

    //锁的路径
    private String basePath;

    private boolean updateTimeOut = true;

    private ThreadLocal<ZookeeperLockSupporter> lockSupportorContext = new ThreadLocal<>();

    private ZookeeperDistributedLock(ZookeeperConfig zookeeperConfig) {
        this.zookeeperConfig = zookeeperConfig;
    }

    static ZookeeperDistributedLock newZookeeperDistributedLock(ZookeeperConfig zookeeperConfig, int supportLockNum) {
        ZookeeperDistributedLock zookeeperDistributedLock = new ZookeeperDistributedLock(zookeeperConfig);
        //初始化zookeeper
        zookeeperDistributedLock.initZookeeper();
        //尝试去请求锁，如果请求zookeeper仓库创建锁，创建成功才继续执行，否则返回null
        if (!zookeeperDistributedLock.accquireLock(supportLockNum)) {
            return null;
        }
        return zookeeperDistributedLock;
    }

    private boolean accquireLock(int supportLockNum) {
        //锁仓库
        String lockRepo = "/lockRepo";
        synchronized (LockUtil.class) {
            try {
                if (zooKeeper.exists(lockRepo, false) == null) {
                    zooKeeper.create(lockRepo, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                //请求锁
                List<String> lockList = zooKeeper.getChildren(lockRepo, null);
                //判断当前仓库是否存在该锁，如果存在，就返回该锁
                if (!lockList.isEmpty()) {
                    String path = existLock(lockList, lockRepo);
                    if (path != null) {
                        basePath = path;
                        return true;
                    }
                }
                //锁仓库里面不存在锁,或者存在锁的数量小于限定的大小直接创建锁
                if (lockList.size() < supportLockNum) {
                    basePath = createLock(lockRepo);
                    if (basePath != null) {
                        return true;
                    }
                }
                //去获得其他可能过期了的锁
                basePath = lootOtherLock(lockRepo, lockList);
                return basePath != null;
            } catch (Exception e) {
                LOGGER.error("创建锁仓库失败", e);
                return false;
            }
        }
    }

    private String existLock(List<String> lockList, String lockRepo) {
        for (String lockPath : lockList) {
            final String path = SimpleUtils.pathJoin(lockRepo, lockPath);
            ZookeeperLocker zookeeperLocker = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<ZookeeperLocker>() {
                @Override
                public ZookeeperLocker run() throws Exception {
                    return ObjectSerializeUtil.readObjFromByte(zooKeeper.getData(path, false, null));
                }
            });
            if (zookeeperConfig.lockConfig().lockName().equals(zookeeperLocker.getLockerName())) {
                //更新lock的信息
                final int version = zookeeperLocker.getVersion().get();
                zookeeperLocker = ZookeeperLocker.newInstance(zookeeperConfig.lockConfig().lockName(), zookeeperConfig.lockConfig().timeOut());
                zookeeperLocker.resetVersion(version + 1);
                final byte[] writeData = ObjectSerializeUtil.writeObj2Byte(zookeeperLocker);
                OperateState op = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<OperateState>() {
                    @Override
                    public OperateState run() throws Exception {
                        zooKeeper.setData(path, writeData, version);
                        return OperateState.SUCCESS;
                    }
                });
                if (op != OperateState.SUCCESS) {
                    throw new CreateLockerException("因为网络原因设置数据失败");
                }
                return path;
            }
        }
        return null;
    }

    /**
     * 抢夺已经过期了的锁
     *
     * @param lockRepo 锁的仓库
     * @param lockList 所有锁的列表
     * @return 被抢夺到的锁
     */
    private String lootOtherLock(String lockRepo, List<String> lockList) {
        for (String lock : lockList) {
            final String path = SimpleUtils.pathJoin(lockRepo, lock);
            List<String> result = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<List<String>>() {
                @Override
                public List<String> run() throws Exception {
                    return zooKeeper.getChildren(path, null);
                }
            });
            if (result == null || !result.isEmpty()) {
                continue;
            }
            //得到当前锁的数据
            ZookeeperLocker zookeeperLocker = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<ZookeeperLocker>() {
                @Override
                public ZookeeperLocker run() throws Exception {
                    return ObjectSerializeUtil.readObjFromByte(zooKeeper.getData(path, false, null));
                }
            });
            if (zookeeperLocker == null) {
                continue;
            }
            //判断当前的锁是否过期
            long lastUpdateTime = zookeeperLocker.getLastUpdateTime();
            if (System.currentTimeMillis() - lastUpdateTime <= zookeeperLocker.getTimeOut()) {
                continue;
            }
            //这把锁已经过期，释放掉
            final int version = zookeeperLocker.getVersion().getAndIncrement();
            //创建新锁的一些信息
            zookeeperLocker = ZookeeperLocker.newInstance(zookeeperConfig.lockConfig().lockName(), zookeeperConfig.lockConfig().timeOut());
            zookeeperLocker.resetVersion(version);
            final byte[] writeDate = ObjectSerializeUtil.writeObj2Byte(zookeeperLocker);
            OperateState operateState = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<OperateState>() {
                @Override
                public OperateState run() throws Exception {
                    zooKeeper.setData(path, writeDate, version);
                    return OperateState.SUCCESS;
                }
            });
            //抢夺锁成功
            if (operateState == OperateState.SUCCESS) {
                return path;
            }
        }
        return null;
    }

    private String createLock(String lockRepo) {
        String internalLock = "/lock-";
        ZookeeperLocker zookeeperLocker = ZookeeperLocker.newInstance(zookeeperConfig.lockConfig().lockName(), zookeeperConfig.lockConfig().timeOut());
        try {
            return zooKeeper.create(lockRepo + internalLock, ObjectSerializeUtil.writeObj2Byte(zookeeperLocker), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
        } catch (Exception e) {
            return null;
        }
    }

    private void initZookeeper() {
        //创建zookeeper
        try {
            zooKeeper = new ZooKeeper(zookeeperConfig.connectStr(), zookeeperConfig.sessionTimeout(), zookeeperConfig.watcher());
            while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
                TimeUnit.MILLISECONDS.sleep(100);
            }
        } catch (Exception e) {
            throw new CreateZookeeperException(e);
        }
    }


    @Override
    public void lock() {
        //更新锁的过期时间
        if (updateTimeOut) {
            synchronized (this) {
                if (updateTimeOut) {
                    ZookeeperLocker zookeeperLocker = getCurrentLockData();
                    if (zookeeperLocker == null) {
                        throw new NetWorkException("网络错误,请调整网络再试");
                    }
                    boolean isValite = zookeeperConfig.lockConfig().lockName().equals(zookeeperLocker.getLockerName()) && (System.currentTimeMillis() - zookeeperLocker.getLastUpdateTime() < zookeeperLocker.getTimeOut());
                    if (!isValite) {
                        //尝试重新去请求下锁
                        if (!accquireLock(LockUtil.MAX_LOCK_NUM)) {
                            throw new LockInValiteException("锁失效，请重新获取");
                        }
                    } else {
                        updateZookeeperData(zookeeperLocker);
                    }
                    updateTimeOut = false;
                }
            }
        }
        ZookeeperLockSupporter supporter = getCurrentThreadSupporter();
        //开始去请求获锁
        supporter.waitLock();
    }


    @Override
    public void unlock() {
        ZookeeperLockSupporter supporter = lockSupportorContext.get();
        if (supporter != null && supporter.isLock()) {
            supporter.releaseLock();
            List<String> list = SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<List<String>>() {
                @Override
                public List<String> run() throws Exception {
                    return zooKeeper.getChildren(basePath, null);
                }
            });
            if (list == null) {
                throw new NetWorkException("网络异常");
            }
            if (list.isEmpty()) {
                //更新时间
                updateZookeeperData(getCurrentLockData());
                updateTimeOut = true;
            }
        }
    }

    /**
     * 得到当前线程在zookeeper上对应节点的支持者
     *
     * @return 支持者
     */
    private ZookeeperLockSupporter getCurrentThreadSupporter() {
        ZookeeperLockSupporter supporter = lockSupportorContext.get();
        if (supporter != null && supporter.isLock()) {
            return supporter;
        }
        if (supporter == null) {
            //创建support
            supporter = new ZookeeperLockSupporter(zooKeeper, basePath, zookeeperConfig.lockConfig().lockName());
        }
        if (supporter.getNodePath() == null) {
            String path = supporter.acquirePath();
            if (path == null) {
                throw new NetWorkException("网络异常");
            }
            //System.out.println(name+"创建节点成功"+path);
            LOGGER.debug("线程{}对应的节点路径是{}", Thread.currentThread(), path);
            lockSupportorContext.set(supporter);
        }
        return supporter;
    }


    /**
     * 获取当前锁上的数据
     *
     * @return 锁的数据
     */
    private ZookeeperLocker getCurrentLockData() {
        return SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<ZookeeperLocker>() {
            @Override
            public ZookeeperLocker run() throws Exception {
                return ObjectSerializeUtil.readObjFromByte(zooKeeper.getData(basePath, false, null));
            }
        });
    }

    /**
     * 更新锁上的数据
     *
     * @param oldData 锁上的老数据
     */
    private void updateZookeeperData(ZookeeperLocker oldData) {
        ZookeeperLocker zookeeperLocker = oldData;
        final int version = zookeeperLocker.getVersion().get();
        zookeeperLocker = ZookeeperLocker.newInstance(zookeeperConfig.lockConfig().lockName(), zookeeperConfig.lockConfig().timeOut());
        zookeeperLocker.resetVersion(version + 1);
        final byte[] writeData = ObjectSerializeUtil.writeObj2Byte(zookeeperLocker);
        //更新时间
        SimpleUtils.asMuchasPossibleRetry(new SimpleUtils.Run<OperateState>() {
            @Override
            public OperateState run() throws Exception {
                zooKeeper.setData(basePath, writeData, version);
                return OperateState.SUCCESS;
            }
        });
    }
}
