package cn.ching.mandal.cluster.support;

import cn.ching.mandal.cluster.Cluster;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.cluster.Directory;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FailoverCluster implements Cluster {

    public static final String NAME = "failover";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailoverClusterInvoker(directory);
    }
}
