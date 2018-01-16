package cn.ching.mandal.rpc.cluster;

import cn.ching.mandal.common.Node;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;

import java.util.List;

/**
 * 2018/1/15
 * cluster directory.
 * find available service from directory.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Directory<T> extends Node{

    /**
     * get service type
     * @return service type
     */
    Class<T> getInterface();

    /**
     * list all invokers
     * @param invocation
     * @return invokers
     * @throws RpcException
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;
}
