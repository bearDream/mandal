package cn.ching.mandal.rpc.cluster;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/2/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface ConfiguratorFactory {

    /**
     * get configurator instance
     * @param url
     * @return
     */
    @Adaptive("protocol")
    Configurator getConfigurator(URL url);
}
