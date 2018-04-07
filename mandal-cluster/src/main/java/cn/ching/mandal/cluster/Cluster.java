package cn.ching.mandal.cluster;

import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.cluster.support.FailoverCluster;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI(FailoverCluster.NAME)
public interface Cluster {

    /**
     * merge the directory invokers to one invoker(virtual invoker)
     * @param directory
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Adaptive
    <T>Invoker<T> join(Directory<T> directory) throws RpcException;
}
