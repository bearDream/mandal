package cn.ching.mandal.monitor.support;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.monitor.api.Monitor;
import cn.ching.mandal.monitor.api.MonitorFactory;
import cn.ching.mandal.monitor.api.support.AbstractMonitorFactory;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * 2018/3/31
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AbstractMonitorFactoryTest {

    private MonitorFactory monitorFactory = new AbstractMonitorFactory() {
        @Override
        protected Monitor createMonitor(URL url) {
            return new Monitor() {
                @Override
                public URL getUrl() {
                    return url;
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public void destroy() {
                }

                @Override
                public void collect(URL statistics) {
                }

                @Override
                public List<URL> lookup(URL query) {
                    return null;
                }
            };
        }
    };

    @Test
    public void testMonitorFactoryCache() throws InterruptedException {
        URL url = URL.valueOf("mandal://" + NetUtils.getLocalAddress().getHostAddress() + ":2233");
        Monitor monitor1 = monitorFactory.getMonitor(url);
        Monitor monitor2 = monitorFactory.getMonitor(url);
        if (monitor1 == null || monitor2 == null){
            Thread.sleep(2000);
            monitor1 = monitorFactory.getMonitor(url);
            monitor2 = monitorFactory.getMonitor(url);
        }
        Assert.assertEquals(monitor1, monitor2);
    }

}
