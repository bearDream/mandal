package cn.ching.mandal.cluster.support;

import cn.ching.mandal.cluster.LoadBalance;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.cluster.Directory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 2018/1/16
 * When fails, record failure request and scheduled retry it.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FailbackClusterInvoker<T> extends AbstractClusterInvoker<T>{

    private static final Logger logger = LoggerFactory.getLogger(FailbackClusterInvoker.class);

    private static final long RETRY_FAILED_PERIOD = 5 * 1000;

    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("failback-cluster-timer"));

    private final ConcurrentMap<Invocation, AbstractClusterInvoker<T>> failed = new ConcurrentHashMap<>();

    private volatile ScheduledFuture retryFuture;

    public FailbackClusterInvoker(Directory directory) {
        super(directory);
    }


    @Override
    protected Result doInvoker(Invocation invocation, List invokers, LoadBalance loadBalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            Invoker<T> invoker = select(loadBalance, invocation, invokers, null);
            return invoker.invoke(invocation);
        }catch (Throwable t){
            logger.error("failed to invoke method: " + invocation.getMethodName() + " from url" + getUrl() + " wait for retry in background . Exception:" + t.getMessage() + ", " + t);
            addFailed(invocation, this);
            return new RpcResult();
        }
    }

    // failed invoke. use scheduled invoke by scheduled.
    private void addFailed(Invocation invocation, AbstractClusterInvoker<T> router) {
        if (Objects.isNull(retryFuture)){
            synchronized (this){
                if (Objects.isNull(retryFuture)){
                    retryFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
                        try {
                            retryFailed();
                        }catch (Throwable t){
                            logger.error("unexpected error occur at collect statistic");
                        }
                    }, RETRY_FAILED_PERIOD, RETRY_FAILED_PERIOD, TimeUnit.MILLISECONDS);
                }
            }
        }
        failed.putIfAbsent(invocation, router);
    }

    private void retryFailed() {
        if (failed.size() == 0){
            return;
        }
        for (Map.Entry<Invocation, AbstractClusterInvoker<?>> entry : new HashMap<Invocation, AbstractClusterInvoker<?>>(failed).entrySet()){
            Invocation invocation = entry.getKey();
            Invoker<?> invoker = entry.getValue();
            try {
                invoker.invoke(invocation);
                failed.remove(invocation);
            }catch (Throwable t){
                logger.error("failed retry to invoke method: " + invocation.getMethodName() + ". waiting again.", t);
            }
        }
    }
}
