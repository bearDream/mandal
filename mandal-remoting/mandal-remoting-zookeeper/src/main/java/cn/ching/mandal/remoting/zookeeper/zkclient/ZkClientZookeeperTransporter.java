package cn.ching.mandal.remoting.zookeeper.zkclient;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.remoting.zookeeper.ZookeeperClient;
import cn.ching.mandal.remoting.zookeeper.ZookeeperTransporter;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ZkClientZookeeperTransporter implements ZookeeperTransporter {

    @Override
    public ZookeeperClient connect(URL url) {
        return new ZkclientZookeeperClient(url);
    }
}
