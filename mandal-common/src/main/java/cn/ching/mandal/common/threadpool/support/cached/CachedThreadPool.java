package cn.ching.mandal.common.threadpool.support.cached;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.threadpool.ThreadPool;
import cn.ching.mandal.common.threadpool.support.AbortPolicyWithReport;

import java.util.concurrent.*;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class CachedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {

        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int cores = url.getParameter(Constants.CORE_THREADS_KEY, Constants.DEFAULT_CORE_THREADS);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        int queue = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
        int alive = url.getParameter(Constants.ALIVE_KEY, Constants.DEFAULT_ALIVE);

        return new ThreadPoolExecutor(cores,
                                        threads,
                                        alive,
                                        TimeUnit.MILLISECONDS,
                                        (queue == 0 ? new SynchronousQueue<Runnable>() : new LinkedBlockingQueue<>(queue)),
                                        new NamedThreadFactory(name),
                                        new AbortPolicyWithReport(name, url));
    }
}
