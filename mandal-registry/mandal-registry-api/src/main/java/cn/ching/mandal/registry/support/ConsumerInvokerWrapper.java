package cn.ching.mandal.registry.support;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.registry.integration.RegistryDirectory;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Result;
import lombok.Getter;

/**
 * 2018/2/6
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConsumerInvokerWrapper<T> implements Invoker {

    private Invoker<T> invoker;
    @Getter
    private URL originUrl;
    @Getter
    private URL registerUrl;
    @Getter
    private URL consumerUrl;
    @Getter
    private RegistryDirectory registryDirectory;

    public ConsumerInvokerWrapper(Invoker<T> invoker, URL registryUrl, URL consumerUrl, RegistryDirectory registryDirectory){
        this.invoker = invoker;
        this.originUrl = URL.valueOf(invoker.getUrl().toFullString());
        this.registerUrl = URL.valueOf(registryUrl.toFullString());
        this.consumerUrl = consumerUrl;
        this.registryDirectory = registryDirectory;
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
