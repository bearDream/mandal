package cn.ching.mandal.rpc.cluster;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface RouterFactory {

    /**
     * create router
     * @param url
     * @return
     */
    @Adaptive("protocol")
    Router getRouter(URL url);
}
