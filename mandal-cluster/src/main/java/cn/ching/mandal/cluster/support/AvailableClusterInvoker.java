package cn.ching.mandal.cluster.support;

import cn.ching.mandal.cluster.Directory;
import cn.ching.mandal.cluster.LoadBalance;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Result;
import cn.ching.mandal.rpc.RpcException;

import java.util.List;

/**
 * 2018/1/16
 * check service available. if one invoker is successful, then return.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AvailableClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AvailableClusterInvoker.class);

    public AvailableClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException {
        checkInvokers(invokers, invocation);
        for (Invoker<T> invoker : invokers) {
            return invoker.invoke(invocation);
        }
        logger.warn("No provider available. " + invokers);
        throw new RpcException("No provider available. " + invokers);
    }
}
