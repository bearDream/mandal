package cn.ching.mandal.common.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * 2018/1/26
 * A {@link Future} that accepts completion listeners.  Each listener has an
 * associated executor, and it is invoked using this executor once the future's
 * computation is {@linkplain Future#isDone() complete}.  If the computation has
 * already completed when the listener is added, the listener will execute
 * immediately.
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface ListenableFuture<T> extends Future<T> {

    /**
     *
     * @param listener the listener to run when the computation is complete
     * @param executor the executor to run the listener in
     * @throws NullPointerException  if the executor or listener was null
     * @throws java.util.concurrent.RejectedExecutionException if we tried to execute the listener immediately but the executor rejected it.
     */
    void addListener(Runnable listener, Executor executor);

    void addListener(Runnable listener);
}
