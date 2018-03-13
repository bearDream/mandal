package cn.ching.mandal.monitor.mandal;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.monitor.api.Monitor;

import java.util.List;

/**
 * 2018/3/13
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalMonitor implements Monitor{
    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
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
}
