package cn.ching.mandal.registry.zookeeper;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.registry.support.AbstractRegistryFactory;
import cn.ching.mandal.remoting.zookeeper.ZookeeperTransporter;
import lombok.Setter;

/**
 * 2018/1/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {

    @Setter
    private ZookeeperTransporter transporter;

    @Override
    protected Registry createRegistry(URL url) {
        return new ZookeeperRegistry(url, transporter);
    }
}
