package cn.ching.mandal.remoting.zookeeper.zkclient;

import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.concurrent.ListenerFutureTask;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.Assert;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 2018/1/26
 * Zkclient wrapper class that can monitor the state of the connection automatically after the connection is out of time
 * It is also consistent with the use of curator
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ZkClientWrapper {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private long timeout;

    private ZkClient client;

    private volatile KeeperState state;

    private ListenerFutureTask<ZkClient> listenerFutureTask;

    private volatile boolean started = false;

    public ZkClientWrapper(final String serverAddress, long timeout){
        this.timeout = timeout;
        listenerFutureTask = ListenerFutureTask.create(() -> {
            return new ZkClient(serverAddress, Integer.MAX_VALUE);
        });
    }

    public void start(){
        if (!started){
            ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 10, 6000L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), new NamedThreadFactory("", true));
            executor.execute(listenerFutureTask);
            try {
                client = listenerFutureTask.get(timeout, TimeUnit.MILLISECONDS);
            }catch (Throwable t){
                logger.error("Timeout! zookeeper server can not be connected in: " + timeout + "ms!", t);
            }
        }else {
            logger.warn("Zkclient has already been started.");
        }
    }

    public void addListener(final IZkStateListener listener){
        listenerFutureTask.addListener(() -> {
            try {
                client = listenerFutureTask.get();
                client.subscribeStateChanges(listener);
            }catch (InterruptedException e){
                logger.warn(Thread.currentThread().getName() + " was interrupted unexpextedly, which may cause unpredictable exception.");
            }catch (ExecutionException e){
                logger.error("when create zk client instance got an exception, can not be connect to zookeeper server, please check!", e);
            }
        });
    }

    public boolean isConnected(){
        return !Objects.isNull(client) && state == KeeperState.SyncConnected;
    }

    public void createPersistent(String path){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        client.createPersistent(path, true);
    }

    public void createEphemeral(String path){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        client.createEphemeral(path);
    }

    public void delete(String path){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        client.delete(path);
    }

    public List<String> getChildren(String path){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        return client.getChildren(path);
    }

    public boolean exists(String path){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        return client.exists(path);
    }

    public void close(){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        client.close();
    }

    public List<String> subscribeChildChanges(String path, final IZkChildListener listener){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        return client.subscribeChildChanges(path, listener);
    }

    public void unsubscribeChildChanges(String path, IZkChildListener listener){
        Assert.notNull(client, new IllegalStateException("zookeeper is not connected yet."));
        client.unsubscribeChildChanges(path, listener);
    }

}
