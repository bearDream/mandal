package cn.ching.mandal.rpc.cluster.configurator.override;

import cn.ching.mandal.rpc.cluster.Configurator;
import cn.ching.mandal.rpc.cluster.ConfiguratorFactory;
import cn.ching.mandal.common.URL;

/**
 * 2018/2/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class OverrideConfiguratorFactory implements ConfiguratorFactory {
    @Override
    public Configurator getConfigurator(URL url) {
        return new OverrideConfigurator(url);
    }
}
