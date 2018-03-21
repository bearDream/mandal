package cn.ching.mandal.common.threadpool.support.fixed;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.threadpool.ThreadPool;
import cn.ching.mandal.common.threadpool.support.AbortPolicyWithReport;

import java.util.concurrent.*;

/**
 * 2018/3/21
 * Creates a thread pool that reuses a fixed number of threads
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class FixedThreadPool implements ThreadPool{
    @Override
    public Executor getExecutor(URL url) {

        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);

        return new ThreadPoolExecutor(threads,
                                        threads,
                                        0,
                                        TimeUnit.MILLISECONDS,
                                        queues == 0 ? new SynchronousQueue<Runnable>() : (queues < 0 ? new LinkedBlockingQueue<Runnable>() : new LinkedBlockingQueue<Runnable>(queues)),
                                        new NamedThreadFactory(name, true),
                                        new AbortPolicyWithReport(name, url));
    }
}
