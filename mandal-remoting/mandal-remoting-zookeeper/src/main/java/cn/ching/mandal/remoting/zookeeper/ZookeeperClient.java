package cn.ching.mandal.remoting.zookeeper;

import cn.ching.mandal.common.URL;

import java.util.List;

/**
 * 2018/1/26
 * zookeeper client.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface ZookeeperClient {

    /**
     * @param path
     * @param ephemeral temporary or persistent connect
     */
    void create(String path, boolean ephemeral);

    void delete(String path);

    List<String> getChildren(String path);

    List<String> addChildListener(String path, ChildListener listener);

    void removeChildListener(String path, ChildListener listener);

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    boolean isConnected();

    void close();

    URL getUrl();
}
