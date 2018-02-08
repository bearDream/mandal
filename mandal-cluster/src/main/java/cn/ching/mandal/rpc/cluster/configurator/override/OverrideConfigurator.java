package cn.ching.mandal.rpc.cluster.configurator.override;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.cluster.configurator.AbstractConfigurator;

/**
 * 2018/2/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class OverrideConfigurator extends AbstractConfigurator {

    public OverrideConfigurator(URL url) {
        super(url);
    }

    @Override
    protected URL doConfigure(URL currentUrl, URL configurUrl) {
        return currentUrl.addParameters(configurUrl.getParameters());
    }
}
