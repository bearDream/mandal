package cn.ching.mandal.rpc;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/3/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface InvokerListener {

    /**
     * invoker referred
     * @param invoker
     * @throws RpcException
     */
    void referred(Invoker<?> invoker) throws RpcException;

    /**
     * invoker destroyed
     * @param invoker
     * @throws RpcException
     */
    void destroyed(Invoker<?> invoker) throws RpcException;
}
