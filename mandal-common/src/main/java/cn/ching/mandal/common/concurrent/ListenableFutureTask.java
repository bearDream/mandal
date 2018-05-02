package cn.ching.mandal.common.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * 2018/3/31
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

    private final ExecutionList executionList = new ExecutionList();

    public static <T> ListenableFutureTask<T> create(Callable<T> callable){
        return new ListenableFutureTask<T>(callable);
    }

    public static <T> ListenableFutureTask<T> create(Runnable runnable, T result){
        return new ListenableFutureTask<T>(runnable, result);
    }

    ListenableFutureTask(Callable<T> callable) {
        super(callable);
    }

    ListenableFutureTask(Runnable runnable, T result) {
        super(runnable, result);
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
