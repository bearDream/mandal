package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.bytecode.Wrapper;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.serialize.support.kryo.utils.ReflectionUtils;
import cn.ching.mandal.common.utils.*;
import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.config.model.ApplicationModel;
import cn.ching.mandal.config.model.ConsumerModel;
import cn.ching.mandal.config.model.ProviderModel;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.ProxyFactory;
import cn.ching.mandal.rpc.StaticContext;
import cn.ching.mandal.rpc.cluster.Cluster;
import cn.ching.mandal.rpc.cluster.directory.StaticDirectory;
import cn.ching.mandal.rpc.cluster.support.AvailableCluster;
import cn.ching.mandal.rpc.cluster.support.ClusterUtils;
import cn.ching.mandal.rpc.injvm.InjvmProtocol;
import cn.ching.mandal.rpc.service.GenericService;
import cn.ching.mandal.rpc.support.ProtocolUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ReferenceConfig<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = 6846610815421107297L;

    private static final Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private final List<URL> urls = new ArrayList<>();

    private String interfaceName;
    private Class<?> interfaceClass;

    private String client;
    // peer-to-peer invocation.
    private String url;

    // method configs
    private List<MethodConfig> methods;
    // default config
    @Getter
    @Setter
    private ConsumerConfig consumer;
    @Getter
    @Setter
    private String protocol;
    // interface proxy reference
    private transient volatile T ref;
    private transient volatile Invoker<?> invoker;
    private transient volatile boolean initialized;
    private transient volatile boolean destroyed;


    private final Object finalizeGuadian = new Object(){
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            if (!ReferenceConfig.this.destroyed){
                logger.warn("ReferenceConfig (" + url + ") is not destroyed when FINALIZE");
            }
        }
    };

    public ReferenceConfig(){}

    public ReferenceConfig(Reference reference){
        appendAnnotation(Reference.class, reference);
    }


    public synchronized T get(){
        if (destroyed){
            throw new IllegalStateException("Already destroyed.");
        }
        if (Objects.isNull(ref)){
            init();
        }
        return ref;
    }

    public synchronized void destroy(){
        if (Objects.isNull(ref)){
            return;
        }
        if (destroyed){
            return;
        }
        destroyed = true;
        try {
            invoker.destroy();
        }catch (Throwable t){
            logger.warn("Unexpected error when destroy invoker of ReferenceConfig(" + url + ").", t);
        }
        invoker = null;
        ref = null;

    }

    private void init() {
        if (initialized){
            return;
        }
        initialized = true;
        if (StringUtils.isEmpty(interfaceName)){
            throw new IllegalStateException("<mandal:reference interface=\" \" /> interface not allow null.");
        }
        checkDefault();
        appendProperties(this);
        if (Objects.isNull(getGeneric()) && Objects.isNull(getConsumer())){
            setGeneric(getConsumer().getGeneric());
        }
        if (ProtocolUtils.isGeneric(getGeneric())){
            interfaceClass = GenericService.class;
        }else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            checkInterfaceAndMethods(interfaceClass, methods);
        }

        // load resolve file.
        String resolve = System.getProperty(interfaceName);
        String resolveFile = null;
        if (StringUtils.isEmpty(resolve)){
            resolveFile = System.getProperty("mandal.resolve.file");
            if (StringUtils.isEmpty(resolveFile)){
                File userResolveFile = new File(new File(System.getProperty("user.home")), "mandal-resolve.properties");
                if (userResolveFile.exists()){
                    resolveFile = userResolveFile.getAbsolutePath();
                }
            }
            if (!StringUtils.isEmpty(resolveFile)){
                Properties properties = new Properties();
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(new File(resolveFile));
                    properties.load(fis);
                } catch (IOException e) {
                    throw new IllegalStateException("Unload " + resolveFile + ", cause by" + e.getMessage(), e);
                }finally {
                    try {
                        if (Objects.isNull(fis)){
                            fis.close();
                        }
                    }catch (IOException e){
                        logger.warn(e.getMessage(), e);
                    }
                }
                resolve = properties.getProperty(interfaceName);
            }
        }
        // warn info.
        if (!StringUtils.isEmpty(resolveFile)){
            url = resolve;
            if (logger.isWarnEnabled()){
                if (!StringUtils.isEmpty(resolveFile)){
                    logger.warn("Using default mandal resolve file " + resolveFile + " replace " + interfaceName + " " + resolve + " to p2p inovoke method name");
                }else {
                    logger.warn("Using -D " + interfaceName + "=" + resolve + " to p2p invoke method name");
                }
            }
        }
        // consumer
        if (!Objects.isNull(consumer)){
            if (Objects.isNull(application)){
                application = consumer.getApplication();
            }
            if (Objects.isNull(module)){
                module = consumer.getModule();
            }
            if (Objects.isNull(registries)){
                registries = consumer.getRegistries();
            }
            if (Objects.isNull(monitor)){
                monitor = consumer.getMonitor();
            }
        }
        // module
        if (!Objects.isNull(module)){
            if (Objects.isNull(registries)){
                registries = module.getRegistries();
            }
            if (Objects.isNull(monitor)){
                monitor = consumer.getMonitor();
            }
        }
        // application
        if (!Objects.isNull(application)){
            if (Objects.isNull(registries)){
                registries = application.getRegistries();
            }
            if (Objects.isNull(monitor)){
                monitor = application.getMonitor();
            }
        }

        checkApplication();
        checkStubAndMock(interfaceClass);
        Map<String, String> map = new HashMap<>();
        Map<Object, Object> attributes = new HashMap<>();
        map.put(Constants.SIDE_KEY, Constants.CONSUMER_SIDE);
        map.put(Constants.MANDAL_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));

        if (ConfigUtils.getPID() > 0){
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPID()));
        }
        // revision
        if (!isGeneric()){
            String revision = Version.getVersion(interfaceClass, version);
            if (!StringUtils.isEmpty(revision)){
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0){
                logger.warn("Mo method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            }else {
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        map.put(Constants.INTERFACE_KEY, interfaceName);
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, consumer, Constants.DEFAULT_KEY);
        appendParameters(map, this);

        String prefix = StringUtils.getServiceKey(map);
        if (!CollectionUtils.isEmpty(methods)){
            methods.forEach(method -> {
                appendParameters(map, method, method.getName());
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)){
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)){
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                appendAttributes(attributes, method, prefix + "." + method.getName());
                checkAndConvertImplicitConfig(method, map, attributes);
            });
        }

        String hostToRegistry = ConfigUtils.getSystemProperty(Constants.MANDAL_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)){
            hostToRegistry = NetUtils.getLocalHost();
        }else if (NetUtils.isInvalidLocalHost(hostToRegistry)){
            throw new IllegalArgumentException("specified invalid registry ip from property:" + Constants.MANDAL_IP_TO_REGISTRY + ".value: " + hostToRegistry);
        }
        map.put(Constants.REGISTRY_KEY, hostToRegistry);

        StaticContext.getSystemContext().putAll(attributes);
        ref = createProxy(map);
        ConsumerModel consumerModel = new ConsumerModel();
        ApplicationModel.initConsumerModel(getUniqueServiceName(), consumerModel);
    }

    private T createProxy(Map<String, String> map) {
        URL tempUrl = new URL("temp", "localhost", 0, map);
        final boolean isJvmRefer;
        // if scope == local, as the service a jvm.
        if (getScopes() == null){
            if (!StringUtils.isEmpty(url)){
                isJvmRefer = false;
            }else if (InjvmProtocol.getInjvmProtocol().isInjvmRefer(tempUrl )){
                isJvmRefer = true;
            }else {
                isJvmRefer = false;
            }
        }else {
            isJvmRefer = getScopes().equals(Constants.LOCAL_KEY);
        }

        if (isJvmRefer){
            URL url = new URL(Constants.LOCAL_PROTOCOL, NetUtils.LOCALHOST, 0, interfaceClass.getName()).addParameters(map);
            invoker = refprotocol.refer(interfaceClass, url);
            if (logger.isInfoEnabled()){
                logger.info("mandal use local service: " + interfaceClass.getName());
            }
        }else {
            if (!StringUtils.isEmpty(url)){ // use peer-to-peer address, or register center.
                String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(url);
                if (!Objects.isNull(us) && us.length > 0){
                    for (String u : us) {
                        URL url = URL.valueOf(u);
                        if (StringUtils.isEmpty(url.getPath())){
                            url.setPath(interfaceName);
                        }
                        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())){
                            urls.add(url.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                        }else {
                            urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }
            }else {  // assembly URL from registry address.
                List<URL> us = loadRegistries(false);
                if (!CollectionUtils.isEmpty(us)){
                    us.stream().forEach(url -> {
                        URL monitorUrl = loadMonitor(url);
                        if (Objects.isNull(monitorUrl)){
                            map.put(Constants.MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                        }
                        urls.add(url.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                    });
                }
                if (CollectionUtils.isEmpty(urls)){
                    throw new IllegalStateException("No such registry reference: " + interfaceName + " on consumer " + NetUtils.getLocalHost() + " use mandal version: " + Version.getVersion() + ". please config mandal registry: <mandal:registry address=\"\" /> to your config file.");
                }
            }

            if (urls.size() == 1){
                invoker = refprotocol.refer(interfaceClass, urls.get(0));
            }else {
                List<Invoker<?>> invokers = new ArrayList<>();
                URL registryUrl = null;
                for (URL u : urls) {
                    invokers.add(refprotocol.refer(interfaceClass, u));
                    if (Constants.REGISTRY_PROTOCOL.equals(u.getProtocol())){
                        registryUrl = u;
                    }
                }

                if (!Objects.isNull(registryUrl)){
                    // use AvailableCluster only when register's cluster is available
                    URL u = registryUrl.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME);
                    invoker = cluster.join(new StaticDirectory(u, invokers));
                }else {
                    invoker = cluster.join(new StaticDirectory(invokers));
                }
            }
        }

        Boolean c = check;
        if (Objects.isNull(c) && !Objects.isNull(consumer)){
            c = consumer.getCheck();
        }
        // default true
        if (Objects.isNull(c)){
            c = true;
        }
        if (c && !invoker.isAvailable()){
            throw new IllegalStateException("failed check the status of the service " + interfaceName + ". No provider available for thr service " + (group == null ? "" : group + "/") + interfaceName + (version == null ? "" : ":" + version) + " from the url " + invoker.getUrl() + " to the consumer " + NetUtils.getLocalHost() + " use mandal version " + Version.getVersion());
        }
        if (logger.isInfoEnabled()){
            logger.info("ref mandal service " + interfaceClass.getName() + " from url:" + invoker.getUrl());
        }

        return (T) proxyFactory.getProxy(invoker);
    }

    private void checkAndConvertImplicitConfig(MethodConfig method, Map<String, String> map, Map<Object, Object> attributes) {
        // check config conflict
        if (Boolean.FALSE.equals(method.isReturn()) && (method.getOnreturn() != null || method.getOnthrow() != null)){
            throw new IllegalStateException("method config error: return attribute must be set true when onreturn or onthrow has been setted.");
        }

        // convert onreturn methodName to Method.
        String onReturnMethodKey = StaticContext.getKey(map, method.getName(), Constants.ON_RETURN_METHOD_KEY);
        Object onReturnMethod = attributes.get(onReturnMethodKey);
        if (!Objects.isNull(onReturnMethod) && onReturnMethod instanceof String) {
            attributes.put(onReturnMethodKey, getMethodByName(method.getOnreturn().getClass(), onReturnMethod.toString()));
        }

        // convert onthrow methodName to Method.
        String onThrowMethodKey = StaticContext.getKey(map, method.getName(), Constants.ON_THROW_METHOD_KEY);
        Object onThrowMethod = attributes.get(onThrowMethodKey);
        if (!Objects.isNull(onThrowMethod) && onThrowMethod instanceof String){
            attributes.put(onThrowMethodKey, getMethodByName(method.getOnthrow().getClass(), onThrowMethod.toString()));
        }

        // convert oninvoke methodName to Method.
        String onInvokeMethodKey = StaticContext.getKey(map, method.getName(), Constants.ON_INVOKE_METHOD_KEY);
        Object onInvokeMethod = attributes.get(onInvokeMethodKey);
        if (!Objects.isNull(onInvokeMethod) && onInvokeMethod instanceof String){
            attributes.put(onInvokeMethodKey, getMethodByName(method.getOninvoke().getClass(), onInvokeMethod.toString()));
        }
    }

    private Object getMethodByName(Class<?> clazz, String methodName) {

        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void checkDefault() {
        if (Objects.isNull(consumer)){
            consumer = new ConsumerConfig();
        }
        appendProperties(consumer);
    }


    @Parameter(exclude = true)
    public String getUniqueServiceName() {
        StringBuffer str = new StringBuffer();
        if (!StringUtils.isEmpty(group)){
            str.append(group).append("/");
        }
        str.append(interfaceName);
        if (!StringUtils.isEmpty(version)){
            str.append(":").append(version);
        }
        return str.toString();
    }

    public Class<?> getInterfaceClass() {
        if (Objects.isNull(interfaceClass)){
            return interfaceClass;
        }
        if (isGeneric() || (getConsumer() != null && getConsumer().isGeneric())){
            return GenericService.class;
        }
        try {
            if (!Objects.isNull(interfaceName) && interfaceName.length() > 0){
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return interfaceClass;
    }

    public String getInterface(){
        return interfaceName;
    }

    public void setInterface(Class<?> interfaceClass){
        if (!Objects.isNull(interfaceClass) && !interfaceClass.isInterface()){
            throw new IllegalStateException("The interface class: " + interfaceClass.getName() + " not interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(Objects.isNull(interfaceClass) ? (String) null : interfaceClass.getName());
    }

    public void setInterface(String interfaceName){
        this.interfaceName = interfaceName;
        if (StringUtils.isEmpty(id)){
            id = interfaceName;
        }
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        checkName("client", client);
        this.client = client;
    }

    @Parameter(exclude = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }
}
