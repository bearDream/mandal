package cn.ching.mandal.cluster.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.cluster.Directory;
import cn.ching.mandal.cluster.LoadBalance;

import java.util.*;

/**
 * 2018/1/15
 * When invoke fails, log the initial error and retry other invokers (retry n times, which means at most n different invokers will be invoked)
 * Note that retry causes latency.
 * @author dubbo
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(Directory<T> directory){
        super(directory);
    }

    /**
     * invoke remote method using loadbalance.
     * because available invokers maybe changed, so use copyInvokers save invokers
     * @param invocation
     * @param invokers
     * @param loadBalance
     * @return invoke result
     * @throws RpcException
     */
    @Override
    protected Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException {

        List<Invoker<T>> copyInvokers = invokers;
        checkInvokers(copyInvokers, invocation);
        int retry = getUrl().getMethodParameter(invocation.getMethodName(), Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES) + 1;
        if (retry <= 0){
            retry = 1;
        }

        RpcException exception = null;
        List<Invoker<T>> invoked = new ArrayList<>(copyInvokers.size());
        Set<String> providers =  new HashSet<>(retry);
        for (int i = 0; i < retry; i++){
            // if "invokers" is changed, then invoked also lose accuracy
            if (i > 0){
                checkWhetherDestryed();
                copyInvokers = list(invocation);
                checkInvokers(copyInvokers, invocation);
            }
            Invoker<T> invoker = select(loadBalance, invocation, copyInvokers, invoked);
            invoked.add(invoker);
            RpcContext.getContext().setInvokers((List) invoked);
            try {
                Result result = invoker.invoke(invocation);
                if (!Objects.isNull(exception) && logger.isWarnEnabled()){
                    logger.warn("Although remote method:" + invocation.getMethodName() + " was successful invoke, but there have been failed provides"
                        + providers + "(" + providers.size() + "/" + copyInvokers.size() + ") from the registry " + directory.getUrl().getAddress()
                        + " on the consumer " + NetUtils.getLocalHost() + " using mandal verison is " + Version.getVersion() + exception.getMessage(), exception);
                }
                return result;
            }catch (RpcException e){
                if (e.isBiz()){
                    throw e;
                }
                exception = e;
            }catch (Throwable t){
                exception = new RpcException(t.getMessage(), t);
            }finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }
        throw new RpcException(Objects.nonNull(exception) ? exception.getCode() : 0, "failed invoke the method" + invocation.getMethodName()
                    + " in the service " + getInterface().getName() + ". tried " + retry + "times of the provider" + providers
                    + "(" + providers.size() + "/" + copyInvokers.size() + "from the registry " + directory.getUrl().getAddress()
                    + " on the consumer " + NetUtils.getLocalHost() + "using the mandal verison is : " + Version.getVersion()
                    + ". last error is :" + (exception != null ? exception.getMessage() : ""), exception != null && exception.getCause() != null ? exception.getCause() : exception);
    }
}
