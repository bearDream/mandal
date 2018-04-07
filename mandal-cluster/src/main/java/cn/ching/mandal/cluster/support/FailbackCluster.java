package cn.ching.mandal.cluster.support;

import cn.ching.mandal.cluster.Cluster;
import cn.ching.mandal.cluster.Directory;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FailbackCluster implements Cluster {

    public static final String name = "failback";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailbackClusterInvoker<T>(directory);
    }
}
