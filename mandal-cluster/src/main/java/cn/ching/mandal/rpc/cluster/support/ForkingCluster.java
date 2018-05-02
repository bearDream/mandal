package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Cluster;
import cn.ching.mandal.rpc.cluster.Directory;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ForkingCluster implements Cluster {

    public final static String NAME = "forking";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new ForkingClusterInvoker<T>(directory);
    }
}