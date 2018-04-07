package cn.ching.mandal.cluster;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;

import java.util.List;

/**
 * 2018/1/15
 * Router
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Router extends Comparable<Router> {

    /**
     * get the router url
     * @return
     */
    URL getUrl();

    /**
     * router
     * @param invokers
     * @param url  refer url
     * @param invocation
     * @param <T>
     * @return  routed invokers
     * @throws RpcException
     */
    <T>List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;
}
