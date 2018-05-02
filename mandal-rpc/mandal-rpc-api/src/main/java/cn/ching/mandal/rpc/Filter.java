package cn.ching.mandal.rpc;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface Filter {
    /**
     * invoker filter
     * <code>
     * // before filter
     * RpcResult res = invoker.invoker(invocation);
     * // after filter
     * return res;
     * </code>
     * @see cn.ching.mandal.rpc.Invoker#invoke(Invocation)
     * @param invoker
     * @param invocation
     * @return
     */
    Result invoker(Invoker<?> invoker, Invocation invocation);
}
