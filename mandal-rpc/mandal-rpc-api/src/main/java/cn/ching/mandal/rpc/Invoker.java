package cn.ching.mandal.rpc;

import cn.ching.mandal.common.Node;

/**
 * 2018/1/5
 *
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