package cn.ching.mandal.remoting.zookeeper.curator;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.remoting.zookeeper.ZookeeperClient;
import cn.ching.mandal.remoting.zookeeper.ZookeeperTransporter;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class CuratorZookeeperTransporter implements ZookeeperTransporter {

    @Override
    public ZookeeperClient connect(URL url) {
        return new CuratorZookeeperClient(url);
    }
}
