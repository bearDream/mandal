package cn.ching.mandal.common.threadpool.support.limited;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.threadpool.ThreadPool;
import cn.ching.mandal.common.threadpool.support.AbortPolicyWithReport;

import java.util.concurrent.*;

/**
 * 2018/3/21
 * Creates a thread pool that creates new threads as needed until limits reaches. This thread pool will not shrink
 * automatically.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class LimitedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {

        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int cores = url.getParameter(Constants.CORE_THREADS_KEY, Constants.DEFAULT_CORE_THREADS);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        int queue = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);

        return new ThreadPoolExecutor(cores,
                                        threads,
                                        Long.MAX_VALUE,
                                        TimeUnit.MILLISECONDS,
                                        (queue == 0 ? new SynchronousQueue<Runnable>() : new LinkedBlockingQueue<Runnable>(queue)),
                                        new NamedThreadFactory(name, true),
                                        new AbortPolicyWithReport(name, url));
    }
}
