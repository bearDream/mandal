package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.LoadBalance;
import cn.ching.mandal.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2018/1/16
 * forking invoke all available service. this way cause resource waste!
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ForkingClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    private final ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory("forking-cluster-timer", true));

    public ForkingClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException {
        checkInvokers(invokers, invocation);
        final List<Invoker<T>> selected;
        // fork thread nums
        final int forks = getUrl().getParameter(Constants.FORKS_KEY, Constants.DEFAULT_FORKS);
        // request timeout
        final int timeout = getUrl().getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        if (forks <= 0 || forks >= invokers.size()){
            selected = invokers;
        }else {
            selected = new ArrayList<Invoker<T>>();
            for (int i = 0; i < forks; i++) {
                Invoker<T> invoker = select(loadBalance, invocation, invokers, selected);
                // avoid add the same invoker.
                if (!selected.contains(invoker)){
                    selected.add(invoker);
                }
            }
        }

        RpcContext.getContext().setInvokers((List) selected);
        final AtomicInteger count = new AtomicInteger();
        final BlockingQueue queue = new LinkedBlockingQueue();
        selected.forEach(invoker ->
            executor.execute(() -> {
                try {
                    Result result = invoker.invoke(invocation);
                    queue.offer(result);
                }catch (Throwable t){
                    int value = count.incrementAndGet();
                    if (value >= selected.size()){
                        queue.offer(t);
                    }
                }
            })
        );
        try {
            Object ref = queue.poll(timeout, TimeUnit.MILLISECONDS);
            if (ref instanceof Throwable){
                Throwable t = (Throwable) ref;
                throw new RpcException(RpcUtils.convertExceptionCode(ref), "failed to forking invoke provider:" + selected + ", but invoke failure. cause by: " + t.getMessage(), Objects.isNull(t.getCause()) ? t : t.getCause());
            }
            return (Result) ref;
        }catch (InterruptedException e) {
            throw new RpcException();
        }

    }
}
