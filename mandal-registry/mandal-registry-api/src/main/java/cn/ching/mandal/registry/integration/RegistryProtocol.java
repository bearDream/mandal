package cn.ching.mandal.registry.integration;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.common.utils.UrlUtils;
import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.registry.RegistryFactory;
import cn.ching.mandal.registry.RegistryService;
import cn.ching.mandal.registry.support.ProviderConsumerRegisterTable;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.cluster.Cluster;
import cn.ching.mandal.cluster.Configurator;
import cn.ching.mandal.rpc.protocol.InvokerWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2018/2/6
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RegistryProtocol implements Protocol{

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    private final int DEFAULT_PORT = 9090;
    private static RegistryProtocol INSTANCE;
    @Getter
    private final Map<URL, NotifyListener> overridesListeners = new ConcurrentHashMap<>();
    private final Map<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<String, ExporterChangeableWrapper<?>>();
    @Setter
    private Cluster cluster;
    @Setter
    private Protocol protocol;
    @Setter
    private RegistryFactory registryFactory;
    @Setter
    private ProxyFactory proxyFactory;

    private RegistryProtocol(){
        INSTANCE = this;
    }

    public static RegistryProtocol getRegistryFactory() {
        if (Objects.isNull(INSTANCE)){
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(Constants.REGISTRY_KEY);
        }
        return INSTANCE;
    }

    public void register(URL registryUrl, URL registedProviderUrl){
        Registry registry = registryFactory.getRegistry(registryUrl);
        registry.register(registedProviderUrl);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> originalInvoker) throws RpcException {
        
        final ExporterChangeableWrapper<T> exporter = doLocalExporter(originalInvoker);

        URL registerUrl = getregistryUrl(originalInvoker);

        // register provider
        final Registry registry = getRegistry(originalInvoker);
        final URL registedProviderUrl = getRegistedProviderUrl(originalInvoker);

        // judge delay publish whether or not
        boolean register = registedProviderUrl.getParameter(Constants.REGISTER_KEY, true);

        // register provider.
        ProviderConsumerRegisterTable.registerProvider(originalInvoker, registerUrl, registedProviderUrl);

        if (register){
            register(registerUrl, registedProviderUrl);
            ProviderConsumerRegisterTable.getProviderWrapper(originalInvoker).setReg(true);
        }

        final URL overrideSubscribeUrl = getSubscribeOverrideUrl(registedProviderUrl);
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originalInvoker);
        overridesListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);

        return new Exporter<T>() {
            @Override
            public Invoker<T> getInvoker() {
                return exporter.getInvoker();
            }

            @Override
            public void unexport() {
                try {
                    exporter.unexport();
                }catch (Throwable t){
                    logger.warn(t.getMessage(), t);
                }
                try {
                    registry.unregister(registedProviderUrl);
                }catch (Throwable t){
                    logger.warn(t.getMessage(), t);
                }
                try {
                    overridesListeners.remove(overrideSubscribeUrl);
                    registry.unsubscribe(overrideSubscribeUrl, overrideSubscribeListener);
                }catch (Throwable t){
                    logger.warn(t.getMessage(), t);
                }
            }
        };
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {

        url = url.setProtocol(url.getParameter(Constants.REGISTER_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTER_KEY);
        Registry registry = registryFactory.getRegistry(url);
        if (RegistryService.class.equals(type)){
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        Map<String, String> queryStr = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        String group = queryStr.get(Constants.GROUP_KEY);
        if (!StringUtils.isEmpty(group)){
            if ((Constants.COMMA_SEPARATOR.split(group).length > 1 || "*".equals(group))){
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }

        return doRefer(cluster, registry, type, url);
    }

    @Override
    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<>(bounds.values());
        exporters.forEach(exporter -> exporter.unexport());
        bounds.clear();
    }

    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        Map<String, String> parameters = new HashMap<>(directory.getUrl().getParameters());
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, parameters.remove(Constants.REGISTER_IP_KEY), 0, type.getName(), parameters);
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface()) && url.getParameter(Constants.REGISTER_KEY, true)){
            registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY, Constants.CHECK_KEY, String.valueOf(false)));
        }
        directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY + "," + Constants.CONFIGURATORS_CATEGORY + "," + Constants.ROUTERS_CATEGORY));
        Invoker invoker = cluster.join(directory);
        // register consumer
        ProviderConsumerRegisterTable.registerConsumer(invoker, url, subscribeUrl, directory);
        return invoker;
    }

    private URL getSubscribeOverrideUrl(URL registedProviderUrl) {
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY, Constants.CACHE_KEY, String.valueOf(false));
    }

    private <T> URL getRegistedProviderUrl(Invoker<T> originalInvoker) {
        URL providerUrl = getProviderUrl(originalInvoker);
        final URL registedProviderUrl = providerUrl.removeParameters(getFilteredKey(providerUrl))
                .removeParameter(Constants.MONITOR_KEY)
                .removeParameter(Constants.BIND_IP_KEY)
                .removeParameter(Constants.BIND_PORT_KEY);
        return registedProviderUrl;
    }

    private String[] getFilteredKey(URL providerUrl) {
        Map<String, String> params = providerUrl.getParameters();
        if (params != null && !params.isEmpty()){
            List<String> filterdKeys = new ArrayList<>();
            params.entrySet().stream()
                    .filter(entry -> !Objects.isNull(entry))
                    .filter(entry -> !Objects.isNull(entry.getKey()))
                    .filter(entry -> entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX))
                    .forEach(entry -> filterdKeys.add(entry.getKey()));
            return filterdKeys.toArray(new String[filterdKeys.size()]);
        }else {
            return new String[]{};
        }
    }

    private <T> Registry getRegistry(Invoker<T> originalInvoker) {
        URL registryUrl = getregistryUrl(originalInvoker);
        return registryFactory.getRegistry(registryUrl);
    }

    private <T> URL getregistryUrl(Invoker<T> originalInvoker) {
        URL registryUrl = originalInvoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())){
            String protocol = registryUrl.getParameter(Constants.REGISTER_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTER_KEY);
        }
        return  registryUrl;
    }

    private <T> ExporterChangeableWrapper<T> doLocalExporter(final Invoker<T> originalInvoker) {
        String key = getCacheKey(originalInvoker);
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (Objects.isNull(exporter)){
            synchronized (bounds){
                exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
                if (Objects.isNull(exporter)){
                    final Invoker<?> invokerDelegate = new InvokerDelegate<T>(originalInvoker, getProviderUrl(originalInvoker));
                    exporter = new ExporterChangeableWrapper<T>((Exporter<T>) protocol.export(invokerDelegate), originalInvoker);
                    bounds.put(key, exporter);
                }
            }
        }
        return exporter;
    }

    private URL getProviderUrl(final Invoker<?> origininvoker){
        String exporter = origininvoker.getUrl().getParameterAndDecoded(Constants.EXPORT_KEY);
        if (StringUtils.isEmpty(exporter)){
            throw new IllegalStateException("The registry export url is null! registry: " + origininvoker.getUrl());
        }
        URL provideUrl = URL.valueOf(exporter);
        return provideUrl;
    }

    private <T> String getCacheKey(Invoker<T> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
        return key;
    }

    private <T> void doChangeLocalExporter(final Invoker<T> originInvoker, URL newInvokeUrl) {
        String key = getCacheKey(originInvoker);
        final ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (Objects.isNull(exporter)){
            logger.warn("error state, exporter should not be null.");
        }else {
            final Invoker<T> invokerDelegate = new InvokerDelegate<>(originInvoker,  newInvokeUrl);
            exporter.setExporter(protocol.export(invokerDelegate));
        }
    }

    public Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }

    private class ExporterChangeableWrapper<T> implements Exporter<T>{

        private final Invoker<T> originInvoker;
        @Getter
        @Setter
        private Exporter<T> exporter;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker){
            this.originInvoker = originInvoker;
            this.exporter = exporter;
        }

        @Override
        public Invoker<T> getInvoker() {
            return originInvoker;
        }

        @Override
        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);
            exporter.unexport();
        }
    }

    private class InvokerDelegate<T> extends InvokerWrapper<T> {

        private final Invoker<T> invoker;

        public InvokerDelegate(Invoker<T> invoker, URL url){
            super(invoker, url);
            this.invoker = invoker;
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegate){
                return ((InvokerDelegate) invoker).getInvoker();
            }
            return invoker;
        }
    }

    /**
     * reexport.
     */
    private class OverrideListener implements NotifyListener{

        private final URL subscribeUrl;
        private final Invoker originInvoker;

        public OverrideListener(URL subscribeUrl, Invoker originInvoker){
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originInvoker;
        }

        private URL getConfiguredInvokerUrl(List<Configurator> configurators, URL url){
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
            return url;
        }

        private List<URL> getMatchedUrl(List<URL> configuratorUrls, URL currentSubscribe){
            List<URL> result = new ArrayList<>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                if (url.getParameter(Constants.CATEGORY_KEY) == null && Constants.OVERRIDE_PROTOCOL.equals(url.getProtocol())){
                    overrideUrl = url.addParameter(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY);
                }

                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)){
                    result.add(url);
                }
            }
            return result;
        }

        /**
         * @param urls The list of registered information , is always not empty, The meaning is the same as the return value of {@link cn.ching.mandal.registry.RegistryService#lookUp(URL)}.
         */
        @Override
        public synchronized void notify(List<URL> urls) {

            List<URL> matchedUrls = getMatchedUrl(urls, subscribeUrl);
            logger.info("subscribe url: " + subscribeUrl + " overrides url: " + matchedUrls);
            if (matchedUrls.isEmpty()){
                return;
            }

            List<Configurator> configurators = RegistryDirectory.toConfigurators(matchedUrls);
            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegate){
                invoker = ((InvokerDelegate) originInvoker).getInvoker();
            }else {
                invoker = originInvoker;
            }

            URL originUrl = RegistryProtocol.this.getProviderUrl(invoker);
            String key = getCacheKey(originInvoker);
            ExporterChangeableWrapper<?> exporter = bounds.get(key);
            if (Objects.isNull(exporter)){
                logger.warn("error state, exporter should not be null");
                return;
            }
            URL currentUrl = exporter.getInvoker().getUrl();
            URL newUrl = getConfiguredInvokerUrl(configurators, originUrl);
            if (!currentUrl.equals(newUrl)){
                RegistryProtocol.this.doChangeLocalExporter(originInvoker, newUrl);
                logger.info("exporter url has changed. origin url:" + originUrl + ", old export url is: " + currentUrl + ", new url is:" + newUrl);
            }
        }
    }
}
