package cn.ching.mandal.registry.zookeeper;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.common.utils.UrlUtils;
import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.registry.support.FailbackRegistry;
import cn.ching.mandal.remoting.zookeeper.ChildListener;
import cn.ching.mandal.remoting.zookeeper.StateListener;
import cn.ching.mandal.remoting.zookeeper.ZookeeperClient;
import cn.ching.mandal.remoting.zookeeper.ZookeeperTransporter;
import cn.ching.mandal.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final static int DEFAULT_ZOOKEEPER_PORT = 2181;

    private final static String DEFAULT_PORT = "mandal";

    private final String root;

    private final Set<String> anyServices = new ConcurrentHashSet<>();

    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<>();

    private final ZookeeperClient client;

    public ZookeeperRegistry(URL url, ZookeeperTransporter transporter) {
        super(url);
        if (url.isAnyHost()){
            throw new IllegalStateException("registry address is null");
        }
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_PORT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)){
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        client = transporter.connect(url);
        client.addStateListener((state) -> {
            if (state == StateListener.RECONNECTED){
                try {
                    recover();
                }catch (Exception e){
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * if address no port then add default port.
     * @param address
     * @return
     */
    static String appendDefaultPort(String address){
        if (!Objects.isNull(address) && address.length() > 0){
            int i = address.indexOf(":");
            if (i < 0){
                return address + ":" + DEFAULT_ZOOKEEPER_PORT;
            }else if (Integer.parseInt(address.substring(i+1)) == 0){
                return address.substring(0, i+1) + DEFAULT_ZOOKEEPER_PORT;
            }
        }
        return address;
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        try {
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())){
                String root = toRootPath();
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                if (Objects.isNull(listeners)){
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                    listeners = zkListeners.get(url);
                }
                ChildListener zkListener = listeners.get(listener);
                if (Objects.isNull(zkListener)){
                    listeners.putIfAbsent(listener, (parentPath, currentChilds) -> {
                       currentChilds.forEach(child -> {
                           child = URL.decode(child);
                           if (!anyServices.contains(child)){
                               anyServices.add(child);
                               subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child, Constants.CHECK_KEY, String.valueOf(false)), listener);
                           }
                       });
                    });
                    zkListener = listeners.get(listener);
                }
                client.create(root, false);
                List<String> services = client.addChildListener(root, zkListener);
                if (!CollectionUtils.isEmpty(services)){
                    services.forEach(service -> {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service, Constants.CHECK_KEY, String.valueOf(false)), listener);
                    });
                }
            }else {
                List<URL> urls = new ArrayList<>();
                for (String path : toCategoriesPath(url)){
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                    if (Objects.isNull(listeners)){
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                        listeners = zkListeners.get(url);
                    }
                    ChildListener zkListener = listeners.get(listener);
                    if (Objects.isNull(zkListener)){
                        listeners.putIfAbsent(listener, (parentPath, currentChild) -> {
                            ZookeeperRegistry.this.notify(url, listener, toUrlWithEmpty(url, parentPath, currentChild));
                        });
                        zkListener = listeners.get(listener);
                    }
                    client.create(path, false);
                    List<String> children = client.addChildListener(path, zkListener);
                    if (!Objects.isNull(children)){
                        urls.addAll(toUrlWithEmpty(url, path, children));
                    }
                }
                notify(urls);
            }
        }catch (Throwable t){
            throw new RpcException("failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause by " + t.getMessage(), t);
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (!Objects.isNull(listeners)){
            ChildListener zkListener = listeners.get(listener);
            if (!Objects.isNull(zkListener)){
                client.removeChildListener(toUrlPath(url), zkListener);
            }
        }
    }

    @Override
    protected void doRegister(URL url) {
        try {
            client.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        }catch (Throwable t){
            throw new RpcException("failed to register " + url + "to zookeeper " + getUrl() + ", cause: " + t.getMessage(), t);
        }
    }

    @Override
    protected void doUnRegister(URL url) {
        try {
            client.delete(toUrlPath(url));
        }catch (Throwable t){
            throw new RpcException("failed to unregister " + url + "to zookeeper " + getUrl() + ", cause: " + t.getMessage(), t);
        }
    }

    @Override
    public List<URL> lookUp(URL url) {
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("lookup url is null");
        }
        try {
            List<String> providers = new ArrayList<>();
            for (String path : toCategoriesPath(url)){
                List<String> children = client.getChildren(path);
                if (Objects.isNull(children)){
                    providers.addAll(children);
                }
            }
            return toUrlsWithoutEmpty(url, providers);
        }catch (Throwable t){
            throw new RpcException("failed to lookup " + url + " from zookeeper" + getUrl() + "cause: " + t.getMessage(), t);
        }
    }

    @Override
    public boolean isAvailable() {
        return client.isConnected();
    }


    private List<URL> toUrlWithEmpty(URL consumer, String paths, List<String> providers) {
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        if (Objects.isNull(urls) || urls.isEmpty()){
            int i = paths.lastIndexOf('/');
            String category = i < 0 ? paths : paths.substring(i + 1);
            URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }

    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<>();
        if (!Objects.isNull(providers) && providers.size() > 0){
            providers.forEach(provider -> {
                provider = URL.decode(provider);
                if (provider.contains("://")){
                    URL url = URL.valueOf(provider);
                    if (UrlUtils.isMatch(consumer, url)){
                        urls.add(url);
                    }
                }
            });
        }
        return urls;
    }

    private String[] toCategoriesPath(URL url) {
        String[] categories;
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))){
            categories = new String[]{
                    Constants.PROVIDERS_CATEGORY, Constants.CONSUMERS_CATEGORY, Constants.ROUTERS_CATEGORY, Constants.CONFIGURATORS_CATEGORY
            };
        }else {
            categories = url.getParameter(Constants.CATEGORY_KEY, new String[]{Constants.DEFAULT_CATEGORY});
        }

        String[] paths = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categories[i];
        }
        return paths;
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)){
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)){
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    private String toRootPath() {
        return root;
    }

    private String toUrlPath(URL url) {
        return toCategoriesPath(url) + Constants.PATH_SEPARATOR + URL.decode(url.toFullString());
    }
}
