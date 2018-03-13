package cn.ching.mandal.common.concurrent;

import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 2018/1/26
 * <p>A list of listeners, each with an associated {@code Executor}, that
 * guarantees that every {@code Runnable} that is {@linkplain #add added} will
 * be executed after {@link #execute()} is called. Any {@code Runnable} added
 * after the call to {@code execute} is still guaranteed to execute. There is no
 * guarantee, however, that listeners will be executed in the order that they
 * are added.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public final class ExecutionList {

    static final Logger logger = LoggerFactory.getLogger(ExecutionList.class.getName());

    private RunnableExectorPair runnables;

    private boolean executed;

    private static final Executor DEFAULT_EXECUTOR = new ThreadPoolExecutor(1, 10, 60000L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), new NamedThreadFactory("MandalFutureCallableDefault", true));

    public ExecutionList(){}

    public void add(Runnable runnable, Executor executor){
        if (Objects.isNull(runnable)){
            throw new NullPointerException("Runable can not be null");
        }
        if (Objects.isNull(executed)){
            logger.info("Executor for listenableFuture is null, will use mandal executor.");
        }

        synchronized (this){
            if (!executed){
                runnables = new RunnableExectorPair(runnable, executor, runnables);
                return;
            }
        }

        executeListener(runnable, executor);
    }

    /**
     * Runs this execution list. executing all existing pairs in the order they
     * were added. However, note that listeners added after this point may be
     * executed before those previously added, and note that the execution order
     * of all listeners is ultimately chosen by the implementations of the
     * supplied executors.
     */
    public void execute() {
        RunnableExectorPair list;
        // Lock while we update our state so the add method above will finish adding
        // any listeners before we start to run them.
        synchronized (this){
            if (executed){
                return;
            }
            executed = true;
            list = runnables;
            runnables = null;
        }
        // build chain
        RunnableExectorPair reversedList = null;
        while (!Objects.isNull(list)){
            RunnableExectorPair tmp = list;
            list = list.next;
            tmp.next = reversedList;
            reversedList = tmp;
        }
        // iterator execute it.
        while (reversedList != null){
            executeListener(reversedList.runnable, reversedList.executor);
            reversedList = reversedList.next;
        }
    }

    private void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        }catch (RuntimeException e){
            logger.error("RuntimeException while executing runnable " + runnable + " with executor " + executor, e);
        }
    }

    private static final class RunnableExectorPair{
        final Runnable runnable;
        final Executor executor;
        RunnableExectorPair next;

        RunnableExectorPair(Runnable runnable, Executor executor, RunnableExectorPair next){
            this.runnable = runnable;
            this.executor = executor;
            this.next = next;
        }
    }
}
