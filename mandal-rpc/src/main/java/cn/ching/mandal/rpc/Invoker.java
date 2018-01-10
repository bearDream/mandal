package cn.ching.mandal.rpc;

/**
 * 2018/1/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Invoker<T> {

    Result invoke (Invocation invocation);
}