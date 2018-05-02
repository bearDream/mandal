package cn.ching.mandal.remoting.zookeeper.zkclient;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.remoting.zookeeper.ChildListener;
import cn.ching.mandal.remoting.zookeeper.StateListener;
import cn.ching.mandal.remoting.zookeeper.support.AbstractZookeeperClient;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import java.util.List;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ZkclientZookeeperClient extends AbstractZookeeperClient<IZkChildListener> {

    private static final Logger logger = LoggerFactory.getLogger(ZkclientZookeeperClient.class);

    private final ZkClientWrapper client;

    private volatile KeeperState state = KeeperState.SyncConnected;

    public ZkclientZookeeperClient(URL url) {
        super(url);
        client = new ZkClientWrapper(url.getBackupAddress(), 30000);
        client.addListener(new IZkStateListener() {
            @Override
            public void handleStateChanged(KeeperState state) throws Exception {
                ZkclientZookeeperClient.this.state = state;
                if (state == KeeperState.Disconnected){
                    stateChanged(StateListener.DISCONNECTED);
                }else if (state == KeeperState.SyncConnected){
                    stateChanged(StateListener.CONNECTED);
                }
            }

            @Override
            public void handleNewSession() throws Exception {
                stateChanged(StateListener.RECONNECTED);
            }
        });
        client.start();
    }

    @Override
    protected void doClose() {
        client.close();
    }

    @Override
    protected void removeTargetChildListener(String path, IZkChildListener iZkChildListener) {
        client.unsubscribeChildChanges(path, iZkChildListener);
    }

    @Override
    protected IZkChildListener createTargetChildListener(String path, ChildListener listener) {
        return ((parentPath, currentChilds) -> {
            listener.childChanged(parentPath, currentChilds);
        });
    }

    @Override
    protected List<String> addTargetChildListener(String path, IZkChildListener iZkChildListener) {
        return client.subscribeChildChanges(path, iZkChildListener);
    }

    @Override
    protected void createPersistent(String path) {
        try {
            client.createPersistent(path);
        }catch (ZkNodeExistsException e){
            logger.warn("createPersistent occured error. " + e.getMessage(), e);
        }
    }

    @Override
    protected void createEphemeral(String path) {
        try {
            client.createEphemeral(path);
        }catch (ZkNodeExistsException e){
            logger.warn("createEphemeral occured error. " + e.getMessage(), e);
        }
    }

    @Override
    protected boolean checkExits(String parentPath) {
        try {
            return client.exists(parentPath);
        }catch (Throwable t){

        }
        return false;
    }

    @Override
    public void delete(String path) {
        try {
            client.delete(path);
        }catch (ZkNodeExistsException e){
            logger.warn("delete zookeeper node occured error. " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getChildren(String path) {
        try {
            return client.getChildren(path);
        }catch (ZkNodeExistsException e){
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        return state == KeeperState.SyncConnected;
    }
}
