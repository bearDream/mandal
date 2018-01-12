package cn.ching.mandal.rpc;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/5
 * ThreadSafe
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("mandal")
public interface Protocol {

    /**
     * if user doesn't set port, then get default port
     * @return default port
     */
    int getDefaultPort();

    /**
     * Export services for remote invocation: <br>
     * 1. Protocol should record request source address after receive a request:
     * RpcContext.getContext().setRemoteAddress();<br>
     * 2. export() must be idempotent, that is, there's no difference between invoking once and invoking twice when
     * export the same URL<br>
     * 3. Invoker instance is passed in by the framework, protocol needs not to care <br>
     * @param invoker Service invoker
     * @param <T> Service Type
     * @return exporter ref for exported services
     * @throws RpcException
     */
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * reference a remote service.
     *
     * 1. When user calls `invoke()` method of `Invoker` object which's returned from `refer()` call, the protocol
     * needs to correspondingly execute `invoke()` method of `Invoker` object <br>
     * 2. It's protocol's responsibility to implement `Invoker` which's returned from `refer()`. Generally speaking,
     * protocol sends remote request in the `Invoker` implementation. <br>
     * 3. When there's check=false set in URL, the implementation must not throw exception but try to recover when
     * connection fails.
     * @param type remote service class
     * @param url  remote service address
     * @param <T>  remote service type
     * @return remote service local proxy
     */
    <T> Invoker<T> refer(Class<T> type, URL url);

    /**
     * destroy protocol
     * 1. cancel all services this protocol exports and refer
     * 2. release all occupied resource.(for example: connection, port)
     * 3. after protocol destroyed export and refer new service
     */
    void destroy();
}
