package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.rpc.cluster.Cluster;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Directory;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FailsafeCluster implements Cluster {

    public static final String NAME = "failsafe";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailsafeClusterInvoker<>(directory);
    }
}
