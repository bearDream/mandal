package cn.ching.mandal.monitor.api;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/3/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("mandal")
public interface MonitorFactory {

    /**
     * create monitor
     * @param url
     * @return
     */
    @Adaptive("protocol")
    Monitor getMonitor(URL url);
}
