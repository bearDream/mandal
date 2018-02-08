package cn.ching.mandal.rpc.cluster;

import cn.ching.mandal.common.URL;

/**
 * 2018/2/6
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Configurator extends Comparable<Configurator> {

    /**
     * get the configurator url
     * @return
     */
    URL getUrl();

    /**
     * configure the provider url
     * @return
     */
    URL configure(URL url);
}
