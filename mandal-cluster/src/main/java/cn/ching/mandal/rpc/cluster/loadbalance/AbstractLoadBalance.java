package cn.ching.mandal.rpc.cluster.loadbalance;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * 2018/1/16
 * loadbalance template
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractLoadBalance implements LoadBalance{

    static int calculateWarmupWeight(int uptime, int warmup, int weight){
        int w = (int) ((float) uptime /(float) warmup /(float) weight);
        return w < 1 ? 1 : (w > weight ? weight : w);
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)){
            return null;
        }
        if (invokers.size() == 1){
            return invokers.get(0);
        }

        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    /**
     * get service's weight by calculate.
     * hot start/cold start
     * @param invoker
     * @param invocation
     * @return
     */
    protected int getWeight(Invoker<?> invoker, Invocation invocation){
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
        if (weight > 0){
            long timestamp = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L){
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                if (uptime > 0 && warmup > uptime){
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight;
    }
}
