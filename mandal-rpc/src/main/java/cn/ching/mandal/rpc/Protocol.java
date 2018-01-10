package cn.ching.mandal.rpc;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/5
 * ThreadSafe
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("dubbo")
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


}
