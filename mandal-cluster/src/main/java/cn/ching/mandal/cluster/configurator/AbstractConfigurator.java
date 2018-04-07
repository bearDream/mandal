package cn.ching.mandal.cluster.configurator;

import cn.ching.mandal.cluster.Configurator;
import cn.ching.mandal.common.URL;

import java.util.Objects;

/**
 * 2018/2/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractConfigurator implements Configurator {

    private final URL configuratorUrl;

    public AbstractConfigurator(URL url){
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("configurator url is null.");
        }
        this.configuratorUrl = url;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public URL configure(URL url) {
        return null;
    }

    @Override
    public int compareTo(Configurator o) {
        return 0;
    }

    protected abstract URL doConfigure(URL currentUrl, URL configurUrl);
}
