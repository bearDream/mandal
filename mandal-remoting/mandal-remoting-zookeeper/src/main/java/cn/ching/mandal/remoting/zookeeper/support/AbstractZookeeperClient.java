package cn.ching.mandal.remoting.zookeeper.support;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.remoting.zookeeper.ChildListener;
import cn.ching.mandal.remoting.zookeeper.StateListener;
import cn.ching.mandal.remoting.zookeeper.ZookeeperClient;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractZookeeperClient<TargetChildListener> implements ZookeeperClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractZookeeperClient.class);

    private final URL url;

    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<>();

    private final ConcurrentMap<String, ConcurrentMap<ChildListener, TargetChildListener>> childListeners = new ConcurrentHashMap<>();

    private volatile boolean closed = false;

    public AbstractZookeeperClient(URL url){
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void create(String path, boolean ephemeral) {
        int i = path.lastIndexOf("/");
        if (i > 0){
            String parentPath = path.substring(0, i);
            if (!checkExits(parentPath)){
                create(parentPath, false);
            }
        }
        if (ephemeral){
            createEphemeral(path);
        }else {
            createPersistent(path);
        }
    }

    @Override
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public Set<StateListener> getSessionListeners(){
        return stateListeners;
    }

    @Override
    public List<String> addChildListener(String path, ChildListener listener) {

        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
        if (Objects.isNull(listeners)){
            childListeners.putIfAbsent(path, new ConcurrentHashMap<ChildListener, TargetChildListener>());
            listeners = childListeners.get(path);
        }
        TargetChildListener targetChildListener = listeners.get(listener);
        if (Objects.isNull(targetChildListener)){
            listeners.putIfAbsent(listener, createTargetChildListener(path, listener));
            targetChildListener = listeners.get(listener);
        }
        return addTargetChildListener(path, targetChildListener);
    }

    @Override
    public void removeChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
        if (!Objects.isNull(listeners)){
            TargetChildListener targetChildListener = listeners.remove(path);
            if (!Objects.isNull(targetChildListener)){
                removeTargetChildListener(path, targetChildListener);
            }
        }
    }

    protected void stateChanged(int state){
        getSessionListeners().forEach(sessionListener -> sessionListener.stateChanged(state));
    }

    @Override
    public void close() {
        if (closed){
            return;
        }
        closed = true;
        try {
            doClose();
        }catch (Throwable t){
            logger.warn("when close zookeeper connection error, cause by: "+t.getMessage(), t);
        }
    }

    protected abstract void doClose();

    protected abstract void removeTargetChildListener(String path, TargetChildListener targetChildListener);

    protected abstract TargetChildListener createTargetChildListener(String path, ChildListener listener);

    protected abstract List<String> addTargetChildListener(String path, TargetChildListener targetChildListener);

    protected abstract void createPersistent(String path);

    protected abstract void createEphemeral(String path);

    protected abstract boolean checkExits(String parentPath);
}

