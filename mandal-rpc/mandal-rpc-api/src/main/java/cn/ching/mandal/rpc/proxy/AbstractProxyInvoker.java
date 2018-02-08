package cn.ching.mandal.rpc.proxy;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractProxyInvoker<T> implements Invoker<T> {

    private final T proxy;

    private final Class<T> type;

    private final URL url;

    public AbstractProxyInvoker(T proxy, Class<T> type, URL url){
        if (Objects.isNull(proxy)){
            throw new IllegalArgumentException("proxy is null");
        }
        if (Objects.isNull(type)){
            throw new IllegalArgumentException("interface is null");
        }
        if (!type.isInterface()){
            throw new IllegalStateException(proxy.getClass().getName() + "not implements interface: " + type);
        }
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }


    @Override
    public Class<T> getInterface() {
        return type;
    }

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
    public Result invoke(Invocation invocation) {
        try {
            return new RpcResult(doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments()));
        }catch (InvocationTargetException e){
            return new RpcResult(e.getTargetException());
        }catch (Throwable t){
            throw new RpcException("failed invoke remote method: " + invocation.getMethodName() + "to" + getUrl() +", cause:" + t.getMessage());
        }
    }

    protected abstract Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] args) throws Throwable;

    @Override
    public String toString() {
        return getInterface() + "->" + (Objects.isNull(getUrl()) ? "  " : getUrl().toString());
    }
}
