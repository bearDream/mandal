package cn.ching.mandal.remoting.zookeeper.zkclient;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.remoting.zookeeper.ChildListener;
import cn.ching.mandal.remoting.zookeeper.support.AbstractZookeeperClient;

import java.util.List;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ZkclientZookeeperClient extends AbstractZookeeperClient {


    public ZkclientZookeeperClient(URL url) {
        super(url);
    }

    @Override
    protected void doClose() {

    }

    @Override
    protected void removeTargetChildListener(String path, Object o) {

    }

    @Override
    protected Object createTargetChildListener(String path, ChildListener listener) {
        return null;
    }

    @Override
    protected List<String> addTargetChildListener(String path, Object o) {
        return null;
    }

    @Override
    protected void createPersistent(String path) {

    }

    @Override
    protected void createEphemeral(String path) {

    }

    @Override
    protected boolean checkExits(String parentPath) {
        return false;
    }

    @Override
    public void delete(String path) {

    }

    @Override
    public List<String> getChildren(String path) {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
