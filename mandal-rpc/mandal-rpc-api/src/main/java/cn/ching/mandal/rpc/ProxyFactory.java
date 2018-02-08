package cn.ching.mandal.rpc;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/15
 * proxy factory
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("jdk")
public interface ProxyFactory {

    /**
     * create proxy
     * @param invoker
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    /**
     * create invoker
     * @param proxy
     * @param type
     * @param url
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;
}
