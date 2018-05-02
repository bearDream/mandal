package cn.ching.mandal.rpc.cluster.configurator.absent;

import cn.ching.mandal.rpc.cluster.configurator.AbstractConfigurator;
import cn.ching.mandal.common.URL;

/**
 * 2018/2/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AbsentConfigurator extends AbstractConfigurator {

    public AbsentConfigurator(URL url){
        super(url);
    }

    @Override
    protected URL doConfigure(URL currentUrl, URL configurUrl) {
        return null;
    }
}
