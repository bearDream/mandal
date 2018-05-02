package cn.ching.mandal.registry.support;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Result;
import lombok.Getter;
import lombok.Setter;

/**
 * 2018/2/6
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProviderInvokerWrapper<T> implements Invoker {

    @Getter
    private Invoker<T> invoker;
    @Getter
    private URL originUrl;
    @Getter
    private URL registerUrl;
    @Getter
    private URL providerUrl;
    @Getter
    @Setter
    private volatile boolean isReg;

    public ProviderInvokerWrapper(Invoker<T> invoker, URL registerUrl, URL providerUrl){
        this.invoker = invoker;
        this.originUrl = URL.valueOf(invoker.getUrl().toFullString());
        this.registerUrl = URL.valueOf(registerUrl.toFullString());
        this.providerUrl = providerUrl;
    }

    @Override
    public Class getInterface() {
        return invoker.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) {
        return invoker.invoke(invocation);
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public void destroy() {
        invoker.destroy();
    }

}
