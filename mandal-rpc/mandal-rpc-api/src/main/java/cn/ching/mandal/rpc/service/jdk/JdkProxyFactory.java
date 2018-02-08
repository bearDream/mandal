package cn.ching.mandal.rpc.service.jdk;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.protocol.AbstractInvoker;
import cn.ching.mandal.rpc.proxy.AbstractProxyFactory;
import cn.ching.mandal.rpc.proxy.AbstractProxyInvoker;
import cn.ching.mandal.rpc.proxy.InvokerInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 2018/1/15
 * jdk proxy factory implement
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class JdkProxyFactory extends AbstractProxyFactory {

    @Override
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) throws RpcException {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, new InvokerInvocationHandler(invoker));
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return new AbstractProxyInvoker<T>(proxy, type, url){

            @Override
            protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] args) throws Throwable {
                Method method = proxy.getClass().getMethod(methodName, parameterTypes);
                return method.invoke(proxy, args);
            }
        };
    }
}
