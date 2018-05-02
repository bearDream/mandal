package cn.ching.mandal.common.concurrent;

import java.util.concurrent.*;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ListenerFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

    private final ExecutionList executionList = new ExecutionList();

    ListenerFutureTask(Callable<T> callable) {
        super(callable);
    }

    ListenerFutureTask(Runnable runnable, T result) {
        super(runnable, result);
    }

    public static <T> ListenerFutureTask<T> create(Callable<T> callable){
        return new ListenerFutureTask<T>(callable);
    }

    public static <T> ListenerFutureTask<T> create(Runnable runnable, T result){
        return new ListenerFutureTask<T>(runnable, result);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        executionList.add(listener, executor);
    }

    @Override
    public void addListener(Runnable listener) {
        executionList.add(listener, null);
    }

    @Override
    protected void done() {
        executionList.execute();
    }
}
