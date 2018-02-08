package cn.ching.mandal.remoting.zookeeper.curator;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.remoting.zookeeper.ChildListener;
import cn.ching.mandal.remoting.zookeeper.StateListener;
import cn.ching.mandal.remoting.zookeeper.support.AbstractZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 2018/1/29
 * curator client for zookeeper server
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class CuratorZookeeperClient extends AbstractZookeeperClient<CuratorWatcher>{

    private static final Logger logger = LoggerFactory.getLogger(CuratorZookeeperClient.class);

    private final CuratorFramework client;

    public CuratorZookeeperClient(URL url) {
        super(url);
        try {
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(url.getBackupAddress())
                    .retryPolicy(new RetryNTimes(1, 1000))
                    .connectionTimeoutMs(5000);
            String authority = url.getAuthority();
            if (!Objects.isNull(authority) && authority.length() > 0){
                builder = builder.authorization("digest", authority.getBytes());
            }
            client = builder.build();
            client.getConnectionStateListenable().addListener((client, state) -> {
                if (state == ConnectionState.LOST){
                    CuratorZookeeperClient.this.stateChanged(StateListener.DISCONNECTED);
                }else if (state == ConnectionState.CONNECTED){
                    CuratorZookeeperClient.this.stateChanged(StateListener.CONNECTED);
                }else if (state == ConnectionState.RECONNECTED){
                    CuratorZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
                }
            });
            client.start();
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void doClose() {
        client.close();
    }

    @Override
    protected void removeTargetChildListener(String path, CuratorWatcher listener) {
        ((CuratorWatcherImpl) listener).unwatch();
    }

    @Override
    protected CuratorWatcher createTargetChildListener(String path, ChildListener listener) {
        return new CuratorWatcherImpl(listener);
    }

    @Override
    protected List<String> addTargetChildListener(String path, CuratorWatcher listener) {
        try {
            return client.getChildren().usingWatcher(listener).forPath(path);
        }catch (KeeperException.NoNodeException e){
            logger.warn("curator addTargetChildListener occured error. path: " + path, e );
            return null;
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void createPersistent(String path) {
        try {
            client.create().forPath(path);
        }catch (KeeperException.NoNodeException e){
            logger.warn("curator create Persistent node occured error. path: " + path, e );
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void createEphemeral(String path) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        }catch (KeeperException.NoNodeException e){
            logger.warn("curator create Ephemeral node occured error. path: " + path, e );
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected boolean checkExits(String path) {
        try {
            if (client.checkExists().forPath(path) != null){
                return true;
            }
        }catch (Exception e){
            logger.warn("curator check client exits occured error. cause by: " + e.getMessage(), e );
        }
        return false;
    }

    @Override
    public void delete(String path) {
        try {
            client.delete().forPath(path);
        }catch (KeeperException.NoNodeException e){
            logger.warn("curator delete node occured error. path: " + path, e );
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getChildren(String path) {
        try {
            return client.getChildren().forPath(path);
        }catch (KeeperException.NoNodeException e){
            logger.warn("curator delete node occured error. path: " + path, e );
            return null;
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    private class CuratorWatcherImpl implements CuratorWatcher{

        private volatile ChildListener listener;

        public CuratorWatcherImpl(ChildListener listener){
            this.listener = listener;
        }

        public void unwatch(){
            listener = null;
        }

        @Override
        public void process(WatchedEvent event) throws Exception {
            if (!Objects.isNull(listener)){
                String path = event.getPath() == null ? "" : event.getPath();
                listener.childChanged(path, StringUtils.isNotEmpty(path) ? client.getChildren().usingWatcher(this).forPath(path) : Collections.emptyList());
            }
        }
    }
}
