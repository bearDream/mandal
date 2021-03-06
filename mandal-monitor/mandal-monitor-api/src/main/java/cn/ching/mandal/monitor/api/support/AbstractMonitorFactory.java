package cn.ching.mandal.monitor.api.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.concurrent.ListenableFuture;
import cn.ching.mandal.common.concurrent.ListenableFutureTask;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.monitor.api.Monitor;
import cn.ching.mandal.monitor.api.MonitorFactory;
import cn.ching.mandal.monitor.api.MonitorService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 2018/3/13
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractMonitorFactory implements MonitorFactory{

    private static final Logger logger = LoggerFactory.getLogger(AbstractMonitorFactory.class);

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final Map<String, Monitor> MONITORS = new ConcurrentHashMap<>();

    private static final Map<String, ListenableFuture<Monitor>> FUTURES = new ConcurrentHashMap<>();

    private static final ExecutorService executor = new ThreadPoolExecutor(0, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory("MandalMonitorCreator", true));

    public static Collection<Monitor> getMonitors(){
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    @Override
    public Monitor getMonitor(URL url) {

        url = url.setPath(MonitorService.class.getName())
                .addParameter(Constants.INTERFACE_KEY, MonitorService.class.getName());
        String key = url.toServiceStringWithoutResolving();
        Monitor monitor = MONITORS.get(key);
        Future<Monitor> future = FUTURES.get(key);
        if (!Objects.isNull(monitor) || !Objects.isNull(future)){
            return monitor;
        }

        LOCK.lock();
        try {

            monitor = MONITORS.get(key);
            future = FUTURES.get(key);
            if (!Objects.isNull(monitor) || !Objects.isNull(future)){
                return monitor;
            }

            final URL monitorUrl = url;
            final ListenableFutureTask<Monitor> listenableFutureTask = ListenableFutureTask.create(new MonitorCreator(monitorUrl));
            listenableFutureTask.addListener(new MonitorListener(key));
            executor.execute(listenableFutureTask);
            FUTURES.put(key, listenableFutureTask);

            return null;
        }finally {
            LOCK.unlock();
        }
    }

    protected abstract Monitor createMonitor(URL url);

    /**
     * Monitor Creator.
     */
    class MonitorCreator implements Callable<Monitor> {

        private URL url;

        public MonitorCreator(URL url) {
            this.url = url;
        }

        @Override
        public Monitor call() throws Exception {
            Monitor monitor = AbstractMonitorFactory.this.createMonitor(url);
            return monitor;
        }
    }

    /**
     * Monitor Listener.
     */
    class MonitorListener implements Runnable {

        private String key;

        public MonitorListener(String key) {
            this.key = key;
        }

        @Override
        public void run() {
            try {
                System.out.println("run it");
                ListenableFuture<Monitor> listenableFuture = AbstractMonitorFactory.FUTURES.get(key);
                AbstractMonitorFactory.MONITORS.put(key, listenableFuture.get());
                AbstractMonitorFactory.FUTURES.remove(key);
            } catch (InterruptedException e) {
                logger.warn("Thread was interrupted unexpectedly, monitor will never get.");
                AbstractMonitorFactory.FUTURES.remove(key);
            } catch (ExecutionException e) {
                logger.warn("Create monitor failed. monitor data will not be collect until you fix the problem.", e);
            }
        }
    }
}
