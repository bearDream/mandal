package cn.ching.mandal.registry.mandal;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.registry.support.FailbackRegistry;

/**
 * 2018/4/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MulticastRegistry extends FailbackRegistry {

    public MulticastRegistry(URL url) {
        super(url);
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {

    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {

    }

    @Override
    protected void doRegister(URL url) {

    }

    @Override
    protected void doUnRegister(URL url) {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
