package cn.ching.mandal.rpc.cluster.directory;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Router;

import java.util.List;
import java.util.Objects;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class StaticDirectory<T> extends AbstractDirectory<T> {

    private final List<Invoker<T>> invokers;

    public StaticDirectory(List<Invoker<T>> invokers) {
        this(null, invokers, null);
    }

    public StaticDirectory(List<Invoker<T>> invokers, List<Router> routers) {
        this(null, invokers, routers);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers) {
        this(url, invokers, null);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers, List<Router> routers) {
        super(Objects.isNull(url) && CollectionUtils.isNotEmpty(invokers) ? invokers.get(0).getUrl() : url , routers);
        if (CollectionUtils.isNotEmpty(invokers)){
            throw new IllegalArgumentException("invokers is null");
        }
        this.invokers = invokers;
    }

    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {
        return invokers;
    }

    @Override
    public Class<T> getInterface() {
        return invokers.get(0).getInterface();
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()){
            return false;
        }
        for (Invoker<T> invoker : invokers) {
            if (invoker.isAvailable()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        if (isDestroyed()){
            return;
        }
        super.destroy();
        invokers.forEach(i -> i.destroy());
        invokers.clear();
    }
}
