package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.rpc.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 2018/1/29
 * abstract proxy protocol.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractProxyProtocol extends AbstractProtocol{

    private final List<Class<?>> rpcExceptions = new CopyOnWriteArrayList<>();

    @Getter
    @Setter
    private ProxyFactory proxyFactory;

    public AbstractProxyProtocol(){}

    public AbstractProxyProtocol(Class<?>... exceptions){
        for (Class<?> exception : exceptions){
            addRpcException(exception);
        }
    }

    public void addRpcException(Class<?> exception) {
        this.rpcExceptions.add(exception);
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        final String uri = serviceKey(invoker.getUrl());
        Exporter<T> exporter = (Exporter<T>) exporterMap.get(uri);
        if (!Objects.isNull(exporter)){
            return exporter;
        }
        final Runnable runnable = doExport(proxyFactory.getProxy(invoker), invoker.getInterface(), invoker.getUrl());
        exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void unexport() {
                super.unexport();
                exporterMap.remove(uri);
                if (!Objects.isNull(runnable)){
                    try {
                        runnable.run();
                    }catch (Throwable t){
                        logger.warn(t.getMessage(), t);
                    }
                }
            }
        };
        exporterMap.put(uri, exporter);
        return exporter;
    }

    @Override
    public <T> Invoker<T> refer(final Class<T> type, final URL url) throws RpcException {
        final Invoker<T> target = proxyFactory.getInvoker(doRefer(type, url), type, url);
        Invoker<T> invoker = new AbstractInvoker<T>(type, url) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                try {
                    Result result = target.invoke(invocation);
                    Throwable t = result.getException();
                    if (!Objects.isNull(t)){
                        rpcExceptions.stream().filter(e -> e.isAssignableFrom(t.getClass())).forEach(e -> {
                            throw getRpcException(type, url, invocation ,t);
                        });
                    }
                    return result;
                }catch (RpcException re){
                    if (RpcException.UNKNOWN_EXCEPTION == re.getCode()){
                        re.setCode(getErrorCode(re.getCause()));
                    }
                    throw re;
                }catch (Throwable t){
                    throw getRpcException(type, url, invocation, t);
                }
            }
        };
        invokers.add(invoker);
        return invoker;
    }

    protected RpcException getRpcException(Class<?> type, URL url, Invocation invocation, Throwable t){
        RpcException exception = new RpcException("failed to invoker remote service: " + type + ", method:"
            + invocation.getMethodName() + ", cause: " + t.getMessage(), t);
        exception.setCode(getErrorCode(t));
        return exception;
    }

    protected String getAddress(URL url){
        String bindIp = url.getParameter(Constants.BIND_IP_KEY, url.getHost());
        if (url.getParameter(Constants.ANYHOST_KEY, false)){
            bindIp = Constants.ANYHOST_VALUE;
        }
        return NetUtils.getIpByHost(bindIp) + ":" + url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
    }

    protected int getErrorCode(Throwable t){
        return RpcException.UNKNOWN_EXCEPTION;
    }

    protected abstract <T> Runnable doExport(T proxy, Class<T> type, URL url)throws RpcException;

    protected abstract <T> T doRefer(Class<T> type, URL url)throws RpcException;
}
