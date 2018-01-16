package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.Objects;

/**
 * 2018/1/16
 * invoker all service one by one. if one error, then throw exception.
 * Only all calls returned successfully !
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class BroadcastClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastClusterInvoker.class);

    public BroadcastClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException {
        checkInvokers(invokers, invocation);
        RpcContext.getContext().setInvokers((List) invokers);
        RpcException exception = null;
        Result result = null;
        for (Invoker<T> invoker : invokers) {
            try {
                result = invoker.invoke(invocation);
            }catch (RpcException e){
                exception = e;
                logger.warn(e.getMessage(), e);
            }catch (Throwable t){
                exception = new RpcException(t.getMessage(), t);
                logger.warn(t.getMessage(), t);
            }
        }
        if (!Objects.isNull(exception)){
            throw exception;
        }
        return result;
    }
}
