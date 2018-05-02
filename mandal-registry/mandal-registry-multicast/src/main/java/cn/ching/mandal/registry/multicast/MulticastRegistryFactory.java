package cn.ching.mandal.registry.multicast;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.registry.support.AbstractRegistryFactory;

/**
 * 2018/4/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MulticastRegistryFactory extends AbstractRegistryFactory{


    @Override
    protected Registry createRegistry(URL url) {
        return new MulticastRegistry(url);
    }
}
