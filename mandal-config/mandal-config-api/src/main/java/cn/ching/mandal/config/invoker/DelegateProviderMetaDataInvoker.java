package cn.ching.mandal.config.invoker;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.config.ServiceConfig;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Result;
import lombok.Getter;

/**
 * 2018/4/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class DelegateProviderMetaDataInvoker<T> implements Invoker {

    protected final Invoker<T> invoker;
    @Getter
    private ServiceConfig metaData;

    public DelegateProviderMetaDataInvoker(Invoker<T> invoker, ServiceConfig metaData){
        this.invoker = invoker;
        this.metaData = metaData;
    }

    @Override
    public Class<T> getInterface() {
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
