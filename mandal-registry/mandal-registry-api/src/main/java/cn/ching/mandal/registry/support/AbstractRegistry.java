package cn.ching.mandal.registry.support;

import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.UrlUtils;
import lombok.Getter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 2018/1/17
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractRegistry implements Registry{

    private static final char URL_SEPARATOR = ' ';

    private static final String URL_SPLIT = "\\s+";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Getter
    private final Properties properties = new Properties();

    private final ExecutorService registryCacheExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("mandal-saved-registryCache", true));

    private final boolean syncSaveFile;

    @Getter
    private final AtomicLong lastCachedChanges = new AtomicLong();

    @Getter
    private final Set<URL> registered = new ConcurrentHashSet<>();

    @Getter
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<>();

    @Getter
    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<>();

    private URL registryUrl;

    @Getter
    private File cacheFile;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractRegistry(URL url){
        setUrl(url);
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);
        String defaultName = System.getProperty("user.home") + "/.mandal/mandal-registry-" + url.getParameter(Constants.APPLICATION_KEY) + "-" + url.getAddress() + ".cache";
        String fileName = url.getParameter(Constants.FILE_KEY, defaultName);
        File file = null;
        if (ConfigUtils.isNotEmpty(fileName)){
            file = new File(fileName);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()){
                if (!file.getParentFile().mkdirs()){
                    throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: faild create directory:" + file.getParentFile() + "!");
                }
            }
        }
        this.cacheFile = file;
        loadProperties();
        notify(url.getBackupUrls());
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)){
            return;
        }
        if (logger.isInfoEnabled()){
            logger.info("destry registry: " + getUrl());
        }
        Set<URL> registries = new HashSet<URL>(getRegistered());
        if (!CollectionUtils.isEmpty(registries)){
            for (URL url : new HashSet<>(registries)) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)){
                    try {
                        unregister(url);
                        if (logger.isInfoEnabled()){
                            logger.info("destroy register: " + url);
                        }
                    }catch (Throwable t){
                        logger.warn("failed unregister url " + url + "exception: " + t.getMessage(), t);
                    }
                }
            }
        }

        Map<URL, Set<NotifyListener>> destroyedSubscribe = new HashMap<>(getSubscribed());
        if (!destroyedSubscribe.isEmpty()){
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroyedSubscribe.entrySet()){
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    if (!Objects.isNull(listener)){
                        try {
                            unsubscribe(url, listener);
                            if (logger.isInfoEnabled()){
                                logger.info("destroy unsubscribe: " + url);
                            }
                        }catch (Throwable t){
                            logger.warn("failed unsubscribe url " + url + "exception: " + t.getMessage(), t);
                        }
                    }
                }
            }
        }

    }

    public void setUrl(URL url) {
        if(Objects.isNull(url)){
            throw new IllegalArgumentException("registryUrl is null");
        }
        this.registryUrl = url;
    }


    @Override
    public List<URL> lookUp(URL url) {
        List<URL> result = new ArrayList<>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (!Objects.isNull(notifiedUrls) && notifiedUrls.size() > 0){
            for (List<URL> urls : notifiedUrls.values()){
                urls.stream().filter(u -> !Constants.EMPTY_PROTOCOL.equals(u.getProtocol())).forEach(u -> {
                    result.add(u);
                });
            }
        }else {
            // if can't find register information, then subscribe and get it.
            final AtomicReference<List<URL>> reference = new AtomicReference<>();
            NotifyListener listener = (urls -> reference.set(urls));
            subscribe(url, listener);
            List<URL> urls = reference.get();
            if (!Objects.isNull(urls) && urls.size() > 0){
                urls.stream().filter(u -> !Constants.EMPTY_PROTOCOL.equals(u.getProtocol())).forEach(u -> {
                    result.add(u);
                });
            }
        }
        return result;
    }

    @Override
    public void register(URL url) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("url is null");
        }
        if (logger.isInfoEnabled()){
            logger.info("registet : " + url);
        }
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("url is null");
        }
        if (logger.isInfoEnabled()){
            logger.info("registet : " + url);
        }
        registered.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("url is null");
        }
        if (Objects.isNull(listener)){
            throw new IllegalArgumentException("listener is null");
        }

        Set<NotifyListener> listeners = subscribed.get(url);
        if (Objects.isNull(listeners)){
            subscribed.put(url, new HashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("url is null");
        }
        if (Objects.isNull(listener)){
            throw new IllegalArgumentException("listener is null");
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (!Objects.isNull(listener)){
            listeners.remove(url);
        }
    }

    /**
     * recover register and subscribe
     * @throws Exception
     */
    protected void recover() throws Exception{
        // register
        Set<URL> register = new HashSet<>(getRegistered());
        if (CollectionUtils.isNotEmpty(register)){
            if (logger.isInfoEnabled()){
                logger.info("recover register :" + register);
            }
            register.stream().forEach(r -> {
                register(r);
            });
        }

        // subscribe
        Map<URL, Set<NotifyListener>> subscribe = new HashMap<>(getSubscribed());
        if (Objects.nonNull(subscribe) && subscribe.size() > 0){
            if (logger.isInfoEnabled()){
                logger.info("recover subscribe :" + register);
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : subscribe.entrySet()){
                URL url = entry.getKey();
                entry.getValue().stream().forEach(listener -> {
                    subscribe(url, listener);
                });
            }
        }
    }

    protected void notify(List<URL> urls){
        if (CollectionUtils.isEmpty(urls)){
            return;
        }

        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()){
            URL url = entry.getKey();

            if (!UrlUtils.isMatch(url, urls.get(0))){
                continue;
            }
            Set<NotifyListener> listeners = entry.getValue();
            if (!Objects.isNull(listeners)){
                listeners.forEach(listener -> {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    }catch (Throwable t){
                        logger.error("failed to notify registry event, cause:" + t.getMessage(), t);
                    }
                });
            }
        }
    }

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("notify url is null");
        }
        if (Objects.isNull(listener)){
            throw new IllegalArgumentException("listener url is null");
        }
        if (CollectionUtils.isEmpty(urls) && !Constants.ANY_VALUE.equals(url.getServiceInterface())){
            logger.warn("ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()){
            logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }
        Map<String, List<URL>> result = new HashMap<>();
        urls.stream().filter(u -> UrlUtils.isMatch(url, u)).forEach(u -> {
            String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            List<URL> categoryList = result.get(category);
            if (Objects.isNull(categoryList)){
                categoryList = new ArrayList<>();
                result.put(category, categoryList);
            }
            categoryList.add(u);
        });
        if (result.size() == 0){
            return;
        }
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null){
            notified.putIfAbsent(url, new ConcurrentHashMap<>());
            categoryNotified = notified.get(url);
        }
        for (Map.Entry<String, List<URL>> entry : result.entrySet()){
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList);
            saveProperties(url);
            listener.notify(categoryList);
        }
        
    }

    private void saveProperties(URL url) {
        if (Objects.isNull(cacheFile)){
            return;
        }
        try {
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (Objects.isNull(categoryNotified)){
                categoryNotified.values().forEach(us -> {
                    us.forEach(u -> {
                        if (buf.length() > 0){
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    });
                });
            }
            properties.setProperty(url.getServiceKey(), buf.toString());
            long version = lastCachedChanges.incrementAndGet();
            if (syncSaveFile){
                doSaveProperty(version);
            }else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        }catch (Throwable t){
            logger.warn(t.getMessage(), t);
        }
    }

    private void doSaveProperty(long version) {
        if (version < lastCachedChanges.get()){
            return;
        }
        if (Objects.isNull(cacheFile)){
            return;
        }
        try {
            File lockFile = new File(cacheFile.getAbsolutePath() + ".lock");
            if (!lockFile.exists()){
                lockFile.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    FileLock fileLock = channel.tryLock();
                    if (Objects.isNull(fileLock)){
                        throw new IOException("couldn't lock the registry cache file " + cacheFile.getAbsolutePath() + ", ignore and retry later. maybe another java thread process use the file. please config: mandal.registry.file=xxx.properties");
                    }
                    try {
                        if (!cacheFile.exists()){
                            cacheFile.createNewFile();
                        }
                        FileOutputStream output = new FileOutputStream(cacheFile);
                        try {
                            properties.store(output, "mandal registry cache");
                        }finally {
                            output.close();
                        }
                    }finally {
                        fileLock.release();
                    }
                }finally {
                    channel.close();
                }
            }finally {
                raf.close();
            }
        }catch (Throwable t){
            if (version < lastCachedChanges.get()){
                return;
            }else {
                // retry write cache file.
                registryCacheExecutor.execute(new SaveProperties(lastCachedChanges.incrementAndGet()));
            }
            logger.warn("failed to save registry store file, cause:" + t.getMessage(), t);
        }
    }

    private static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)){
            List<URL> res = new ArrayList<>();
            res.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return res;
        }
        return urls;
    }

    protected void loadProperties(){
        if (!Objects.isNull(cacheFile) && cacheFile.exists()){
            InputStream in = null;
            try {
                in = new FileInputStream(cacheFile);
                properties.load(in);
                if (logger.isInfoEnabled()){
                    logger.info("load registry store file " + cacheFile + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("failed to load registry store file " + cacheFile);
            }finally {
                if (!Objects.isNull(in)){
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    };

    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!Objects.isNull(key) && key.length() > 0 && key.equals(url.getServiceKey())
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_')
                    && value != null && value.length() > 0){
                String[] s = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<>();
                for (String u : s) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getUrl().toString();
    }

    private class SaveProperties implements Runnable{

        private long version;
        private SaveProperties(long version){
            this.version = version;
        }
        @Override
        public void run() {
            doSaveProperty(version);
        }
    }
}
