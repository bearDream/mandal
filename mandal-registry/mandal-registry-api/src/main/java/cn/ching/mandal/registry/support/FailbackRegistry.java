package cn.ching.mandal.registry.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.registry.NotifyListener;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2018/1/20
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class FailbackRegistry extends AbstractRegistry {

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1,  new NamedThreadFactory("mandal-failback-registry-retry"));

    @Getter
    private final ScheduledFuture<?> retryFuture;

    @Getter
    private final Set<URL> failedRegistry = new HashSet<URL>();

    @Getter
    private final Set<URL> failedUnRegistry = new HashSet<URL>();

    @Getter
    private final ConcurrentMap<URL, Set<NotifyListener>> failedSubscribe = new ConcurrentHashMap<>();

    @Getter
    private final ConcurrentMap<URL, Set<NotifyListener>> failedUnSubscribe = new ConcurrentHashMap<>();

    @Getter
    private final ConcurrentMap<URL, Map<NotifyListener, List<URL>>> failedNotified = new ConcurrentHashMap<>();

    @Getter
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public FailbackRegistry(URL url) {
        super(url);
        int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
        retryFuture = executor.scheduleWithFixedDelay(() -> {
            try {
                retry();
            }catch (Throwable t){
                logger.warn("Unexpected error at failed retry. {}"+t.getMessage(), t);
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (destroyed.get()){
            return;
        }
        super.subscribe(url, listener);
        removeFailedSubscribe(url, listener);
        try {
            doSubscribe(url, listener);
        }catch (Throwable t){

            List<URL> urls = getCacheUrls(url);
            if (CollectionUtils.isNotEmpty(urls)){
                notify(url, listener, urls);
                logger.error("failed to subscribe " + url + ", using cache urls: " + urls + "from cache file: " + getUrl().getParameter(Constants.FILE_KEY, "user.home" + "/mandal-registry-" + url.getHost() + ".cache" + ", cause: " + t.getMessage()), t);
            }else {
                boolean check = getUrl().getParameter(Constants.CHECK_KEY, true) && url.getParameter(Constants.CHECK_KEY, true);
                boolean skipFailback = t instanceof SkipFailbackWrapperException;

                if (check || skipFailback){
                    if (skipFailback){
                        t = t.getCause();
                    }
                    throw new IllegalStateException("failed subscrib : " + url + "cause: " + t.getMessage(), t);
                }else {
                    logger.error("failed to subscribe " + url + " waiting for retry");
                }
            }

            addFailedSubscribe(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (destroyed.get()){
            return;
        }
        super.unsubscribe(url, listener);
        removeFailedSubscribe(url, listener);
        try {
            doUnsubscribe(url, listener);
        }catch (Throwable t){

            List<URL> urls = getCacheUrls(url);
            if (CollectionUtils.isNotEmpty(urls)){
                notify(url, listener, urls);
                logger.error("failed to unsubscrib " + url + ", using cache urls: " + urls + "from cache file: " + getUrl().getParameter(Constants.FILE_KEY, "user.home" + "/mandal-registry-" + url.getHost() + ".cache" + ", cause: " + t.getMessage()), t);
            }else {
                boolean check = getUrl().getParameter(Constants.CHECK_KEY, true) && url.getParameter(Constants.CHECK_KEY, true);
                boolean skipFailback = t instanceof SkipFailbackWrapperException;

                if (check || skipFailback){
                    if (skipFailback){
                        t = t.getCause();
                    }
                    throw new IllegalStateException("failed unsubscrib : " + url + "cause: " + t.getMessage(), t);
                }else {
                    logger.error("failed to unsubscrib " + url + " waiting for retry");
                }
            }

            Set<NotifyListener> listeners = failedUnSubscribe.get(url);
            if (Objects.isNull(listener)){
                failedUnSubscribe.putIfAbsent(url, new HashSet<>());
                listeners = failedUnSubscribe.get(url);
            }
            listeners.add(listener);
        }
    }

    @Override
    public void register(URL url) {
        if (destroyed.get()){
            return;
        }
        super.register(url);
        failedUnRegistry.remove(url);
        failedRegistry.remove(url);
        try {
            doRegister(url);
        }catch (Throwable t){
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailback = t instanceof SkipFailbackWrapperException;

            if (check || skipFailback){
                if (skipFailback){
                    t = t.getCause();
                }
                throw new IllegalStateException("failed to register " + url + "cause: " + t.getMessage(), t);
            }else {
                logger.error("failed to register " + url + "waiting for retry");
            }

            failedRegistry.add(url);
        }
    }

    @Override
    public void unregister(URL url) {
        if (destroyed.get()){
            return;
        }

        super.unregister(url);
        failedRegistry.remove(url);
        failedUnRegistry.remove(url);
        try {
            doUnRegister(url);
        }catch (Throwable t){
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailback = t instanceof SkipFailbackWrapperException;

            if (check || skipFailback){
                if (skipFailback){
                    t = t.getCause();
                }
                throw new IllegalStateException("failed to unregister " + url + "cause: " + t.getMessage(), t);
            }else {
                logger.error("failed to unregister " + url + "waiting for retry");
            }

            failedUnRegistry.add(url);
        }
    }

    @Override
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("url is null");
        }
        if (Objects.isNull(listener)){
            throw new IllegalArgumentException("listener is null");
        }
        try {
            doNotify(url, listener, urls);
        }catch (Throwable t){
            Map<NotifyListener, List<URL>> listeners = failedNotified.get(url);
            if (Objects.isNull(listeners)){
                failedNotified.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, List<URL>>());
                listeners = failedNotified.get(url);
            }
            listeners.put(listener, urls);
            logger.error("failed notify to subscribe: " + url + ", waiting for retry. cause: " + t.getMessage(), t);
        }
    }

    protected void doNotify(URL url, NotifyListener listener, List<URL> urls){
        super.notify(url, listener, urls);
    }


    @Override
    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegister = new HashSet<>(getRegistered());
        if (!recoverRegister.isEmpty()){
            if (logger.isInfoEnabled()){
                logger.info("recover register url:" + recoverRegister);
            }
            recoverRegister.forEach(recover -> failedRegistry.add(recover));
        }

        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribe = new ConcurrentHashMap(getSubscribed());
        if (!recoverSubscribe.isEmpty()){
            if (logger.isInfoEnabled()){
                logger.info("recover subscribe url: " + recoverSubscribe);
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribe.entrySet()) {
                URL url = entry.getKey();
                entry.getValue().forEach(listener -> addFailedSubscribe(url, listener));
            }
        }
    }

    @Override
    public void destroy() {
        if (!canDestroy()){
            return;
        }
         super.destroy();
        try {
            retryFuture.cancel(true);
        }catch (Throwable t){
            logger.warn("failed to destroy: "+t.getMessage(), t);
        }
    }

    protected boolean canDestroy(){
        if (destroyed.compareAndSet(false, true)){
            return true;
        }
        return false;
    }

    private void removeFailedSubscribe(URL url, NotifyListener listener) {

        Set<NotifyListener> listeners = failedSubscribe.get(url);
        if (!Objects.isNull(listeners)){
            listeners.remove(url);
        }

        listeners = failedUnSubscribe.get(url);
        if (!Objects.isNull(listeners)){
            listeners.remove(url);
        }

        Map<NotifyListener, List<URL>> notify = failedNotified.get(url);
        if (!Objects.isNull(notify)){
            notify.remove(listener);
        }
    }

    private void addFailedSubscribe(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribe.get(url);
        if (Objects.isNull(listeners)){
            failedSubscribe.putIfAbsent(url, new HashSet<NotifyListener>());
            listeners = failedSubscribe.get(url);
        }
        listeners.add(listener);
    }

    private void retry() {
        // retry registry
        if (CollectionUtils.isNotEmpty(failedRegistry)){
            Set<URL> fails = new HashSet<>(failedRegistry);
            if (logger.isInfoEnabled()){
                logger.warn("retry registry:" + fails);
            }
            try {
                for (URL fail : fails) {
                    try {
                        doRegister(fail);
                        failedRegistry.remove(fail);
                    }catch (Throwable t){
                        logger.warn("failed to retry register : " + fail + " wait to retry it. cause: " + t.getMessage(), t);
                    }
                }
            }catch (Throwable t){
                logger.warn("failed to retry register :" + fails + " try again later. cause:" + t.getMessage(), t);
            }
        }

        // retry unregistry
        if (CollectionUtils.isNotEmpty(failedUnRegistry)){
            Set<URL> fails = new HashSet<>(failedUnRegistry);
            if (logger.isInfoEnabled()){
                logger.warn("retry unregistry:" + fails);
            }
            try {
                fails.forEach(fail -> {
                    try {
                        doUnRegister(fail);
                        failedUnRegistry.remove(fail);
                    }catch (Throwable t){
                        logger.warn("failed to retry unregistry : " + fail + " wait to retry it. cause: " + t.getMessage(), t);
                    }
                });
            }catch (Throwable t){
                logger.warn("failed to retry unregister :" + fails + " try again later. cause:" + t.getMessage(), t);

            }
        }

        // retry subscribe
        if (failedSubscribe.size() > 0){
            Map<URL, Set<NotifyListener>> fails = new HashMap<>(failedSubscribe);
            if (logger.isInfoEnabled()){
                logger.warn("retry subscribe:" + fails);
            }
            try {
                for (Map.Entry<URL, Set<NotifyListener>> entry : fails.entrySet()) {
                    URL url = entry.getKey();
                    Set<NotifyListener> listeners = entry.getValue();
                    for (NotifyListener listener : listeners) {
                        try {
                            doSubscribe(url, listener);
                            failedSubscribe.remove(url);
                        }catch (Throwable t){
                            logger.warn("failed to retry subscribe : " + fails + " wait to retry it. cause: " + t.getMessage(), t);
                        }
                    }
                }
            }catch (Throwable t){
                logger.warn("failed to retry subscribe :" + fails + " try again later. cause:" + t.getMessage(), t);

            }
        }

        // retry unsubscribe
        if (failedUnSubscribe.size() > 0){
            Map<URL, Set<NotifyListener>> fails = new HashMap<>(failedUnSubscribe);
            if (logger.isInfoEnabled()){
                logger.warn("retry unsubscribe:" + fails);
            }
            try {
                for (Map.Entry<URL, Set<NotifyListener>> entry : fails.entrySet()) {
                    URL url = entry.getKey();
                    Set<NotifyListener> listeners = entry.getValue();
                    for (NotifyListener listener : listeners) {
                        try {
                            doUnsubscribe(url, listener);
                            failedUnSubscribe.remove(url);
                        }catch (Throwable t){
                            logger.warn("failed to retry unsubscribe : " + fails + " wait to retry it. cause: " + t.getMessage(), t);
                        }
                    }
                }
            }catch (Throwable t){
                logger.warn("failed to retry unsubscribe :" + fails + " try again later. cause:" + t.getMessage(), t);

            }
        }

        // retry notify
        if (failedNotified.size() > 0){
            Map<URL, Map<NotifyListener, List<URL>>> fails = new HashMap<>(failedNotified);
            for (Map.Entry<URL, Map<NotifyListener, List<URL>>> entry : fails.entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0){
                    failedNotified.remove(entry.getKey());
                }
            }
            
            if (logger.isInfoEnabled()){
                logger.warn("retry unsubscribe:" + fails);
            }
            try {
                for (Map<NotifyListener, List<URL>> values : fails.values()) {
                    for (Map.Entry<NotifyListener, List<URL>> entry : values.entrySet()) {
                        try {
                            NotifyListener listener = entry.getKey();
                            List<URL> urls = entry.getValue();
                            listener.notify(urls);
                            failedNotified.remove(listener);
                        }catch (Throwable t){
                            logger.warn("failed to retry notified : " + fails + " wait to retry it. cause: " + t.getMessage(), t);
                        }
                    }
                }
            }catch (Throwable t){
                logger.warn("failed to retry notified : " + fails + " try again later. cause:" + t.getMessage(), t);
            }

        }
    }


    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

    protected abstract void doRegister(URL url);

    protected abstract void doUnRegister(URL url);
}
