package cn.ching.mandal.rpc.cluster.configurator.absent;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.cluster.Configurator;
import cn.ching.mandal.rpc.cluster.ConfiguratorFactory;

/**
 * 2018/2/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AbsentConfiguratorFactory implements ConfiguratorFactory{
    @Override
    public Configurator getConfigurator(URL url) {
        return null;
    }
}
