package cn.ching.mandal.cluster.support;

import cn.ching.mandal.cluster.Directory;
import cn.ching.mandal.cluster.LoadBalance;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Result;
import cn.ching.mandal.rpc.RpcException;

import java.util.List;

/**
 * 2018/1/16
 * failfast.
 * execute exactly once. which means this policy will throw an exception immediately in case of an invocation error.
 * Usually used for non-idempotent write operations
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FailfastClusterInvoker<T> extends AbstractClusterInvoker<T>{

    private static final Logger logger = LoggerFactory.getLogger(FailfastClusterInvoker.class);

    public FailfastClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException {
        checkInvokers(invokers, invocation);
        Invoker<T> invoker = select(loadBalance, invocation, invokers, null);
        try {
            return invoker.invoke(invocation);
        }catch (Throwable t){
            if (t instanceof RpcException && ((RpcException) t).isBiz()){
                throw (RpcException) t;
            }
            throw new RpcException(t instanceof RpcException ? ((RpcException) t).getCode() : 0, "failed invoke providers " + invoker.getUrl() + " " + loadBalance.getClass().getSimpleName() + "select from all provides " + invokers + "for service " + getInterface().getName() + " method: " + invocation.getMethodName() + " on consumer " + NetUtils.getLocalHost() + " use mandal verison is " + Version.getVersion() + ", but invoke failed, cause by: " + t.getMessage(), t.getCause() != null ? t.getCause() : t);
        }
    }
}
