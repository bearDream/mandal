package cn.ching.mandal.rpc;

import cn.ching.mandal.common.Node;

/**
 * 2018/1/5
 * AbstractClusterInvoker(客户端集群Invoker)  -->  AbstractInvoker(客户端Invoker)  -->  AbstractProxyInvoker(反射执行方法)
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Invoker<T> extends Node{

    /**
     * get service interface
     * @return service interface
     */
    Class<T> getInterface();

    /**
     * do invoke
     * @param invocation
     * @return
     */
    Result invoke (Invocation invocation);
}