package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FailsafeClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailsafeClusterInvoker.class);

    public FailsafeClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            Invoker<T> invoker = select(loadBalance, invocation, invokers, null);
            return invoker.invoke(invocation);
        }catch (Throwable t){
            logger.error("failsafe ignore exception: " + t.getMessage(), t);
            return new RpcResult();
        }
    }
}
