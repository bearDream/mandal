package cn.ching.mandal.registry.integration;

import cn.ching.mandal.cluster.*;
import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.cluster.*;
import cn.ching.mandal.cluster.directory.AbstractDirectory;
import cn.ching.mandal.cluster.directory.StaticDirectory;
import cn.ching.mandal.cluster.support.ClusterUtils;
import cn.ching.mandal.rpc.protocol.InvokerWrapper;
import cn.ching.mandal.rpc.support.RpcUtils;
import lombok.Setter;

import java.util.*;

/**
 * 2018/2/6
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);

    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    private static final ConfiguratorFactory CONFIGURATOR_FACTORY = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();

    private static final RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();

    private final String serviceKey;
    private final Class<T> serviceType;
    private final Map<String, String> queryMap;
    private final URL directoryUrl;
    private final String[] serviceMethods;
    private final boolean multiGroup;
    @Setter
    private Protocol protocol; // Initialization at the time of injection
    @Setter
    private Registry registry; // Initialization at the time of injection
    private volatile boolean forbidden = true;

    private volatile URL overrideDirectoryUrl;

    private volatile List<Configurator> configurators;

    private volatile Map<String, Invoker<T>> urlInvokerMap;

    private volatile Map<String, List<Invoker<T>>> methodsInvokerMap;

    private volatile Set<URL> cachedInvokerUrls;

    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if (Objects.isNull(serviceType)) {
            throw new IllegalArgumentException("serviceType is null.");
        }
        if (StringUtils.isEmpty(url.getServiceKey())){
            throw new IllegalArgumentException("registry serviceKey is null.");
        }
        this.serviceType = serviceType;
        this.serviceKey = url.getServiceKey();
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        this.directoryUrl = url.setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(Constants.MONITOR_KEY);
        this.overrideDirectoryUrl = this.directoryUrl;
        String group = directoryUrl.getParameter(Constants.GROUP_KEY, "");
        this.multiGroup = !Objects.isNull(group) && ("*".equals(group) || group.contains(","));
        String methods = queryMap.get(Constants.METHOD_KEY);
        this.serviceMethods = Objects.isNull(methods) ? null : Constants.COMMA_SPLIT_PATTERN.split(methods);
    }

    @Override
    public void notify(List<URL> urls) {
        List<URL> invokerUrls = new ArrayList<>();
        List<URL> routerUrls = new ArrayList<>();
        List<URL> configuratorUrls = new ArrayList<>();
        for (URL url : urls){
            String protocol = url.getProtocol();
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            if (Constants.ROUTERS_CATEGORY.equals(category) || Constants.ROUTE_PROTOCOL.equals(protocol)){
                routerUrls.add(url);
            }else if(Constants.CONFIGURATORS_CATEGORY.equals(category) || Constants.OVERRIDE_PROTOCOL.equals(protocol)){
                configuratorUrls.add(url);
            }else if (Constants.PROVIDERS_CATEGORY.equals(category)){
                invokerUrls.add(url);
            }else {
                logger.warn("Unsupported category " + category + " in notified url: " + " from registry " + getUrl().getAddress() + "to consumer " + NetUtils.getLocalHost());
            }
        }
        if (!CollectionUtils.isEmpty(configuratorUrls)){
            this.configurators = toConfigurators(configuratorUrls);
        }
        // router
        if (!CollectionUtils.isEmpty(routerUrls)){
            List<Router> routers = toRouters(routerUrls);
            if (!Objects.isNull(routers)){
                setRouters(routers);
            }
        }
        List<Configurator> localConfigurators = this.configurators;
        this.overrideDirectoryUrl = directoryUrl;
        if (!CollectionUtils.isEmpty(localConfigurators)){
            localConfigurators.forEach(configurator -> this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl));
        }

        refreshInvoker(invokerUrls);
    }

    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {
        // no service provider; service providers are disabled. throw exception.
        if (forbidden){
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION, "No provider available from registry "
                + getUrl().getAddress() + " for service " + getConsumerUrl().getServiceKey()
                + " on consumer " + NetUtils.getLocalHost() + " use mandal version is "
                + Version.getVersion() + ", may be provider disabled or not registerd");
        }
        List<Invoker<T>> invokers = null;
        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodsInvokerMap;
        if (!CollectionUtils.isEmpty(localMethodInvokerMap)){
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArgument(invocation);
            if (!Objects.isNull(args) && args.length > 0 && !Objects.isNull(args[0]) && (args[0] instanceof String || args[0].getClass().isEnum())){
                invokers = localMethodInvokerMap.get(methodName + "." + args[0]);
            }
            if (Objects.isNull(invokers)){
                invokers = localMethodInvokerMap.get(methodName);
            }
            if (Objects.isNull(invokers)){
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }
            if (Objects.isNull(invokers)){
                Iterator<List<Invoker<T>>> iterator = localMethodInvokerMap.values().iterator();
                if (iterator.hasNext()){
                    invokers = iterator.next();
                }
            }
        }
        return Objects.isNull(invokers) ? new ArrayList<Invoker<T>>(0) : invokers;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()){
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (!Objects.isNull(localUrlInvokerMap) && localUrlInvokerMap.size() > 0){
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())){
                if (invoker.isAvailable()){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        if (isDestroyed()){
            return;
        }
        try {
            if (!Objects.isNull(getConsumerUrl()) && !Objects.isNull(registry) && registry.isAvailable()){
                registry.unsubscribe(getConsumerUrl(), this);
            }
        }catch (Throwable t){
            logger.warn("unexported error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        super.destroy();
        try {
            destroyAllInvokers();
        }catch (Throwable t){
            logger.warn("failed to destroy service " + serviceKey, t);
        }
    }

    public void subscribe(URL url){
        setConsumerUrl(url);
        registry.subscribe(url, this);
    }


    private void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
        if (!Objects.isNull(localUrlInvokerMap)){
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())){
                try {
                    invoker.destroy();
                }catch (Throwable t){
                    logger.warn("failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        methodsInvokerMap = null;
    }

    private List<Router> toRouters(List<URL> urls) {
        List<Router> routers = new ArrayList<>();
        if (CollectionUtils.isEmpty(urls)){
            return routers;
        }
        if (CollectionUtils.isNotEmpty(urls)){
            for (URL url : urls) {
                if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())){
                    continue;
                }
                String routerType = url.getParameter(Constants.ROUTER_KEY);
                if (!StringUtils.isEmpty(routerType)){
                    url = url.setProtocol(routerType);
                }
                try {
                    Router router = routerFactory.getRouter(url);
                    if (!routers.contains(router)){
                        routers.add(router);
                    }
                }catch (Throwable t){
                    logger.error("convert router url to router error, url: " + url, t);
                }
            }
        }
        return routers;
    }

    /**
     * refresh invoker.
     * 1、if invokerUrls is empty. It means that the rule is only a override rule or route rule, which needs to be re-contrashed to decide whether to re-refrence.
     * 2、if invokerUrls is not empty. it means that it is the latest invoker list.
     * 3、if
     * @param invokerUrls
     */
    private void refreshInvoker(List<URL> invokerUrls) {
        if (!Objects.isNull(invokerUrls)
                && invokerUrls.size() == 1
                && !Objects.isNull(invokerUrls.get(0))
                && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())){
            this.forbidden = true;
            this.methodsInvokerMap = null;
            destroyAllInvokers();
        }else {
            this.forbidden = false;
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap;
            if (!CollectionUtils.isEmpty(invokerUrls)){
                invokerUrls.addAll(this.cachedInvokerUrls);
            }else {
                this.cachedInvokerUrls = new HashSet<>();
                this.cachedInvokerUrls.addAll(invokerUrls);
            }
            if (invokerUrls.size() == 0){
                return;
            }
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);
            Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap);

            // state changed
            if (Objects.isNull(newUrlInvokerMap) || newUrlInvokerMap.size() == 0){
                logger.error(new IllegalStateException("url to invokers error .invokerUrls.size :" + invokerUrls.size() + ", invoker.size :0, urls :" + invokerUrls.toString()));
                return;
            }
            this.methodsInvokerMap = multiGroup ? toMergeMethodInvokerMap(newMethodInvokerMap) : newMethodInvokerMap;
            this.urlInvokerMap = newUrlInvokerMap;
            try {
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap);
            }catch (Exception e){
                logger.warn("destroyUnusedInvokers errors, ", e);
            }
        }
    }

    /**
     * merge multi method invoker.
     * @param methodMap
     * @return
     */
    private Map<String,List<Invoker<T>>> toMergeMethodInvokerMap(Map<String, List<Invoker<T>>> methodMap) {
        Map<String, List<Invoker<T>>> result = new HashMap<>();
        for (Map.Entry<String, List<Invoker<T>>> entry : methodMap.entrySet()) {
            String method = entry.getKey();
            List<Invoker<T>> invokers = entry.getValue();
            Map<String, List<Invoker<T>>> groupMap = new HashMap<>();
            invokers.stream().forEach(invoker -> {
                String group = invoker.getUrl().getParameter(Constants.GROUP_KEY, "");
                List<Invoker<T>> groupInvokers = groupMap.get(group);
                if (Objects.isNull(groupInvokers)){
                    groupInvokers = new ArrayList<>();
                    groupMap.put(group, groupInvokers);
                }
                groupInvokers.add(invoker);
            });
            if (groupMap.size() == 1){
                result.put(method, groupMap.values().iterator().next());
            }else if (groupMap.size() == 2){
                List<Invoker<T>> groupInvokers = new ArrayList<>();
                groupMap.values().forEach(groupList -> {
                    groupInvokers.add(cluster.join(new StaticDirectory<T>(groupList)));
                });
                result.put(method, groupInvokers);
            }else {
                result.put(method, invokers);
            }
        }
        return result;
    }

    private Map<String,List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<>();
        List<Invoker<T>> invokerList = new ArrayList<>();
        if (!Objects.isNull(invokersMap) && invokersMap.size() > 0){
            invokersMap.values().forEach(invoker -> {
                String parameter = invoker.getUrl().getParameter(Constants.METHOD_KEY);
                if (!StringUtils.isEmpty(parameter)){
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (!Objects.isNull(methods) && methods.length > 0){
                        for (String method : methods) {
                            if (!StringUtils.isEmpty(method) && !Constants.ANY_VALUE.equals(method)){
                                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                if (Objects.isNull(methodInvokers)){
                                    methodInvokers = new ArrayList<>();
                                    newMethodInvokerMap.put(method, methodInvokers);
                                }
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }
                invokerList.add(invoker);
            });
        }

        List<Invoker<T>> newInvokersList = route(invokerList, null);
        newMethodInvokerMap.put(Constants.ANY_VALUE, newInvokersList);
        if (!Objects.isNull(serviceMethods) && serviceMethods.length > 0){
            for (String method : serviceMethods) {
                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                if (Objects.isNull(methodInvokers) || methodInvokers.size() == 0){
                    methodInvokers = newInvokersList;
                }
                newMethodInvokerMap.put(method, route(methodInvokers, method));
            }
        }
        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
            Collections.sort(methodInvokers, ((o1, o2) -> o1.getUrl().toString().compareTo(o2.getUrl().toString())));
            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(newMethodInvokerMap);
    }

    private List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {
        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        List<Router> routers = getRouters();
        if (!Objects.isNull(routers)){
            for (Router router : routers) {
                if (!Objects.isNull(router)){
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }

    /**
     * Convert url to invoker. If url has been refer, will not re-reference.
     *
     * @param urls
     * @return invokers
     */
    private Map<String,Invoker<T>> toInvokers(List<URL> urls) {
        Map<String,Invoker<T>> newUrlInvokerMap = new HashMap<>();
        if (CollectionUtils.isEmpty(urls)){
            return newUrlInvokerMap;
        }
        Set<String> keys = new HashSet<>();
        String queryProtocols = this.queryMap.get(Constants.PROTOCOL_KEY);
        for (URL providerUrl : urls) {
            if (!StringUtils.isEmpty(queryProtocols)){
                boolean accept = false;
                String[] acceptProtocols = queryProtocols.split(",");
                for (String acceptProtocol : acceptProtocols) {
                    if (providerUrl.getProtocol().equals(acceptProtocol)){
                        accept = true;
                        break;
                    }
                }
                if (!accept){
                    continue;
                }
            }
            if (Constants.EMPTY_PROTOCOL.equals(providerUrl.getProtocol())){
                continue;
            }
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())){
                logger.error("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: "
                        + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost()
                        + ", supported protocol: " + ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions());
                continue;
            }
            URL url = mergeUrl(providerUrl);

            String key = url.toFullString();
            if (keys.contains(key)){
                continue;
            }
            keys.add(key);


            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
            Invoker<T> invoker = Objects.isNull(localUrlInvokerMap) ? null : localUrlInvokerMap.get(key);
            if (Objects.isNull(invoker)){
                try {
                    boolean enabled = true;
                    if (url.hasParameter(Constants.DISABLED_KEY)){
                        enabled = !url.getParameter(Constants.DISABLED_KEY, false);
                    }else {
                        enabled = url.getParameter(Constants.DISABLED_KEY, true);
                    }
                    if (enabled){
                        invoker = new InvokerDelegate<T>(protocol.refer(serviceType, url), url, providerUrl);
                    }
                }catch (Throwable t){
                    logger.error("Failed to refer invoker for interface: " + serviceType + ", url:[" + url + "]" + t.getMessage(), t);
                }
                if (!Objects.isNull(invoker)){
                    newUrlInvokerMap.put(key, invoker);
                }
            }else {
                newUrlInvokerMap.put(key, invoker);
            }
        }
        keys.clear();
        return newUrlInvokerMap;
    }

    /**
     * Merge url parameters.
     * order: overrider > -D > Consumer > Provider
     * @param providerUrl
     * @return
     */
    private URL mergeUrl(URL providerUrl) {

        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap);

        List<Configurator> localConfigurators = this.configurators;
        if (!Objects.isNull(localConfigurators) && localConfigurators.size() > 0){
            for (Configurator configurator : localConfigurators) {
                configurator.configure(providerUrl);
            }
        }

        providerUrl = providerUrl.addParameter(Constants.CHECK_KEY, String.valueOf(false));

        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters());
        if ((Objects.isNull(providerUrl.getPath()) || providerUrl.getPath().length() == 0) && "mandal".equals(providerUrl.getProtocol())){
            String path = directoryUrl.getParameter(Constants.INTERFACE_KEY);
            if (!Objects.isNull(path)){
                int i = path.indexOf("/");
                if (i >= 0){
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0){
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }
        return providerUrl;
    }

    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap){
        if (Objects.isNull(newUrlInvokerMap) || newUrlInvokerMap.size()==0){
            destroyAllInvokers();
            return;
        }
        List<String> deleted = null;
        if (!Objects.isNull(oldUrlInvokerMap)){
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                if (!newInvokers.contains(entry.getValue())){
                    if (Objects.isNull(deleted)){
                        deleted = new ArrayList<>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }

        if (Objects.isNull(deleted)){
            deleted.stream()
                    .filter(url -> !Objects.isNull(url))
                    .forEach(url -> {
                        Invoker<T> invoker = oldUrlInvokerMap.get(url);
                        if (!Objects.isNull(invoker)){
                            try {
                                invoker.destroy();
                                if (logger.isDebugEnabled()){
                                    logger.debug("destroy invoker[" + invoker.getUrl() + "] success.");
                                }
                            }catch (Exception e){
                                logger.warn("destroy invoker[" + invoker.getUrl() + "] failed. " + e.getMessage(), e);
                            }
                        }
                    });
        }
    }

    /**
     * Convert override urls to map for use when re-refer.
     * Send all rules.
     *
     * @param urls
     *             </br>1.override://0.0.0.0/...( or override://ip:port...?anyhost=true)&para1=value1... means global rules (all of the providers take effect)
     *             </br>2.override://ip:port...?anyhost=false Special rules (only for a certain provider)
     *             </br>3.override:// rule is not supported... ,needs to be calculated by registry itself.
     *             </br>4.override://0.0.0.0/ without parameters means clearing the override
     * @return
     */
    public static List<Configurator> toConfigurators(List<URL> urls) {

        if (CollectionUtils.isEmpty(urls)){
            return Collections.emptyList();
        }

        List<Configurator> configurators = new ArrayList<>(urls.size());
        for (URL url : urls) {
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())){
                configurators.clear();
                break;
            }

            Map<String, String> overrides = new HashMap<>(url.getParameters());
            overrides.remove(Constants.ANYHOST_KEY);
            if (overrides.size() == 0){
                configurators.clear();
                continue;
            }
            configurators.add(CONFIGURATOR_FACTORY.getConfigurator(url));
        }
        Collections.sort(configurators);
        return configurators;
    }

    private static class InvokerDelegate<T> extends InvokerWrapper<T>{
        private URL providerUrl;

        public InvokerDelegate(Invoker<T> invoker, URL url, URL providerUrl){
            super(invoker, url);
            this.providerUrl = providerUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }
    }
}
