package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.bytecode.Wrapper;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.*;
import cn.ching.mandal.config.annoatation.Service;
import cn.ching.mandal.config.invoker.DelegateProviderMetaDataInvoker;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.cluster.ConfiguratorFactory;
import cn.ching.mandal.rpc.service.GenericService;
import cn.ching.mandal.rpc.support.ProtocolUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ServiceConfig<T> extends AbstractServiceConfig{

    private static final long serialVersionUID = -3356538077473533827L;

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<>();

    private static final ScheduledExecutorService delayExportExcutors = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MandalServiceDelayExporter", true));

    private final List<URL> urls = new ArrayList<>();

    private final List<Exporter<?>> exporters = new ArrayList<>();
    // interface type
    private String interfaceName;
    private Class<?> interfaceClass;
    // reference to interface impl
    private T ref;
    private String path;
    // method config
    private List<MethodConfig> methods;
    @Getter
    @Setter
    private ProviderConfig provider;
    private transient volatile boolean exported;

    private transient volatile boolean unexported;

    private volatile String generic;

    public ServiceConfig(){}

    public ServiceConfig(Service service){
        appendAnnotation(Service.class, service);
    }

    public static Integer getRandomPort(String protocol) {
        protocol = protocol.toLowerCase();
        if (RANDOM_PORT_MAP.containsKey(protocol)){
            return RANDOM_PORT_MAP.get(protocol);
        }
        return Integer.MIN_VALUE;
    }

    public static void putRandomPort(String protocol, Integer port){
        protocol = protocol.toLowerCase();
        if (!RANDOM_PORT_MAP.containsKey(protocol)){
            RANDOM_PORT_MAP.put(protocol, port);
        }
    }

    public URL toUrl(){
        return CollectionUtils.isEmpty(urls) ? null : urls.get(0);
    }

    public List<URL> toUrls(){
        return urls;
    }

    @Parameter(exclude = true)
    public boolean isExported() {
        return exported;
    }

    @Parameter(exclude = true)
    public boolean isUnexported() {
        return unexported;
    }

    public String getInterface() {
        return interfaceName;
    }

    @Parameter(exclude = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        checkPathName("path", path);
        this.path = path;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (Objects.isNull(id) || id.length() == 0){
            id = interfaceName;
        }
    }

    public synchronized void export(){
        if (!Objects.isNull(provider)){
            if (Objects.isNull(export)){
                export = provider.getExport();
            }
            if (Objects.isNull(delay)){
                delay = provider.getDelay();
            }
        }

        if (!Objects.isNull(export) && !export){
            return;
        }
        if (!Objects.isNull(delay) && delay > 0){
            delayExportExcutors.schedule(() -> {
                doExport();
            }, delay, TimeUnit.MILLISECONDS);
        }else {
            doExport();
        }

    }

    protected synchronized void doExport() {

        if (unexported){
            throw new IllegalStateException("Already unexported.");
        }

        if (export){
            return;
        }

        export = true;
        if (StringUtils.isEmpty(interfaceName)){
            throw new IllegalStateException("<mandal:service interface=\"\"" + " /> interface name is null!");
        }
        checkDefault();
        if (!Objects.isNull(provider)){
            if (Objects.isNull(application)){
                application = provider.getApplication();
            }
            if (Objects.isNull(module)){
                module = provider.getModule();
            }
            if (Objects.isNull(registries)){
                registries = provider.getRegistries();
            }
            if (Objects.isNull(monitor)){
                monitor = provider.getMonitor();
            }
            if (Objects.isNull(protocols)){
                protocols = provider.getProtocols();
            }
        }
        if (!Objects.isNull(module)){
            if (Objects.isNull(registries)){
                registries = module.getRegistries();
            }
            if (Objects.isNull(monitor)){
                monitor = module.getMonitor();
            }
        }
        if (!Objects.isNull(application)){
            if (Objects.isNull(registries)){
                registries = application.getRegistries();
            }
            if (Objects.isNull(monitor)){
                monitor = application.getMonitor();
            }
        }
        if (ref instanceof GenericService){
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)){
                generic = Boolean.TRUE.toString();
            }
        }else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            }catch (ClassNotFoundException e){
                throw new IllegalStateException(e.getMessage(), e);
            }
            checkInterfaceAndMethods(interfaceClass, methods);
            checkRef();
            generic = Boolean.FALSE.toString();
        }
        if (!Objects.isNull(local)){
            if ("true".equals(local)){
                local = interfaceName + "Local";
            }
            Class<?> localClass;
            try {
                localClass = ClassHelper.forNameWithThreadContextClassLoader(local);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(localClass)){
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
            }
        }
        if (!Objects.isNull(stub)){
            if ("true".equals(stub)){
                stub = interfaceName + "Stub";
            }
            Class<?> stubClass;
            try {
                stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(stubClass)){
                throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }
        checkApplication();
        checkRegistry();
        checkProtocol();
        appendProperties(this);
        checkStubAndMock(interfaceClass);
        if (StringUtils.isEmpty(path)){
            path = interfaceName;
        }
        doExportUrls();
    }

    private void doExportUrls() {
        List<URL> registriesURLs = loadRegistries(true);
        for (ProtocolConfig protocolConfig : protocols) {
            doExportUrlsFor1Protocol(protocolConfig, registriesURLs);
        }
    }

    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryUrls){
        String serviceName = protocolConfig.getName();
        if (StringUtils.isEmpty(serviceName)){
            serviceName = "mandal";
        }

        Map<String, String> map = new HashMap<>();
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
        map.put(Constants.MANDAL_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPID() > 0){
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPID()));
        }
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY);
        appendParameters(map, protocolConfig);
        appendParameters(map, this);

        if (CollectionUtils.isEmpty(methods)){
            for (MethodConfig method : methods) {
                appendParameters(map, method, method.getName());
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)){
                    String retryValue = map.remove(retryKey);
                    if ("false".equalsIgnoreCase(retryValue)){
                        map.put(method.getName() + ".retries", "0");
                    }
                }

                List<ArgumentConfig> argumentConfigs = method.getArguments();
                if (!CollectionUtils.isEmpty(argumentConfigs)){
                    for (ArgumentConfig argumentConfig : argumentConfigs) {
                        if (!StringUtils.isEmpty(argumentConfig.getType())){
                            Method[] methods = interfaceClass.getMethods();
                            if (methods != null && methods.length > 0){
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();

                                    if (methodName.equals(method.getName())){
                                        Class<?>[] argTypes = methods[i].getParameterTypes();

                                        // one call back in the method.
                                        if (argumentConfig.getIndex() != -1){
                                            if (argTypes[argumentConfig.getIndex()].getName().equals(argumentConfig.getType())){
                                                appendParameters(map, argumentConfig, method.getName() + "." + argumentConfig.getIndex());
                                            }else {
                                                throw new IllegalArgumentException("argument config error: the index attribute and type attribute not match :index : " + argumentConfig.getIndex() + " :type:" + argumentConfig.getType());
                                            }
                                        }else {
                                            // multiple call back in the method.
                                            for (int j = 0; j < argTypes.length; j++) {
                                                Class<?> argClazz = argTypes[j];
                                                if (argClazz.getName().equals(argumentConfig.getType())){
                                                    appendParameters(map, argumentConfig, method.getName() + "." + j);
                                                    if (argumentConfig.getIndex() != -1 && argumentConfig.getIndex() != j){
                                                        throw new IllegalArgumentException("argument config error. the index attribute and type attribute not match :index : " + argumentConfig.getIndex() + ", type" + argumentConfig.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }else if (argumentConfig.getIndex() != -1){
                            appendParameters(map, argumentConfig, method.getName() + "." + argumentConfig.getIndex());
                        }else {
                            throw new IllegalArgumentException("argument must set index or type attribute. like: <mandal:argument index='0' ... /> or <mandal:argument type='xxx' ... />");
                        }
                    }
                }

            }
        }

        if (ProtocolUtils.isGeneric(generic)){
            map.put(Constants.GENERIC_KEY, generic);
            map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
        }else {
            String revision = Version.getVersion(interfaceClass, version);
            if (!StringUtils.isEmpty(revision)){
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0){
                logger.warn("No methods found in interface: " + interfaceClass.getName());
                map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
            }else {
                map.put(Constants.METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }

        if (!ConfigUtils.isEmpty(token)){
            if (ConfigUtils.isDefault(token)){
                map.put(Constants.TOKEN_KEY, UUID.randomUUID().toString());
            }else {
                map.put(Constants.TOKEN_KEY, token);
            }
        }

        if ("injvm".equals(protocolConfig.getName())){
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }
        // export service
        String contextPath = protocolConfig.getContextpath();
        if (StringUtils.isEmpty(contextPath) && !Objects.isNull(provider)){
            contextPath = provider.getContextPath();
        }

        String host = this.findConfigedHost(protocolConfig, registryUrls, map);
        Integer port = this.findConfigedPort(protocolConfig, serviceName, map);
        URL url = new URL(serviceName, host, port, (StringUtils.isEmpty(contextPath) ? "" : contextPath + "/") + path, serviceName);

        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).hasExtension(url.getProtocol())){
            url = ExtensionLoader
                    .getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol())
                    .getConfigurator(url)
                    .configure(url);
        }

        String scope = url.getParameter(Constants.SCOPE_KEY);

        // if config scope is none, then don't export.
        if (!Constants.SCOPE_NONE.equalsIgnoreCase(scope)){

            // if config scope is local, then export local.
            if (Constants.SCOPE_LOCAL.equalsIgnoreCase(scope)){
                exportLocal(url);
            }

            if (Constants.SCOPE_REMOTE.equalsIgnoreCase(scope)){
                if (logger.isInfoEnabled()){
                    logger.info("Export Mandal service: " + interfaceClass.getName() + " to url: " + url);
                }
                // multiple registryUrl.
                if (!CollectionUtils.isEmpty(registryUrls)){
                    for (URL registryUrl : registryUrls) {
                        url = url.addParameterIfAbsent(Constants.DYNAMIC_KEY, registryUrl.getParameter(Constants.DYNAMIC_KEY));
                        URL monitorUrl = loadMonitor(registryUrl);
                        if (!Objects.isNull(monitorUrl)){
                            url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                        }

                        if (logger.isInfoEnabled()){
                            logger.info("Register Mandal service " + interfaceClass.getName() + " url " + url + " to registry " + registryUrl);
                        }
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class<T>) interfaceClass, registryUrl.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                        Exporter<?> exporter = protocol.export(invoker);
                        exporters.add(exporter);
                    }
                }else {
                    // single registryUrl.
                    Invoker<T> invoker = proxyFactory.getInvoker(ref, (Class<T>) interfaceClass, url);
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                    Exporter<?> exporter = protocol.export(invoker);
                    exporters.add(exporter);
                }
            }
        }
        this.urls.add(url);
    }

    /**
     * configure bind Ip address for service provider, and Register.
     * Configurator priority: environment variables -> java system properties -> host property in config file ->
     * /etc/hosts -> default network address -> first available network address
     * @param protocolConfig
     * @param registryUrls
     * @param map
     * @return
     */
    private String findConfigedHost(ProtocolConfig protocolConfig, List<URL> registryUrls, Map<String, String> map) {
        boolean anyhost = false;

        // 1.environment variables
        String hostToBind = getValueFromConfigurator(protocolConfig, Constants.MANDAL_IP_TO_BIND);
        if (StringUtils.isEmpty(hostToBind) && NetUtils.isInvalidLocalHost(hostToBind)){
            throw new IllegalArgumentException("Specified invalid bind ip from property " + Constants.MANDAL_IP_TO_BIND + ".value " + hostToBind);
        }

        if (StringUtils.isEmpty(hostToBind)){
            // 2. java system properties
            hostToBind = protocolConfig.getHost();
            if (StringUtils.isEmpty(hostToBind) && !Objects.isNull(provider)){
                // 3. host property in config file
                hostToBind = provider.getHost();
            }
            if (NetUtils.isInvalidLocalHost(hostToBind)){
                try {
                    // 4. /etc/hosts
                    hostToBind = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.warn(e.getMessage(), e);
                }

                if (NetUtils.isInvalidLocalHost(hostToBind)){
                    // 5. default network address
                    if (!CollectionUtils.isEmpty(registryUrls)){
                        for (URL registryUrl : registryUrls) {
                            try {
                                Socket socket = new Socket();
                                try {
                                    SocketAddress add = new InetSocketAddress(registryUrl.getHost(), registryUrl.getPort());
                                    socket.connect(add);
                                    hostToBind = socket.getLocalAddress().getHostAddress();
                                }finally {
                                    try {
                                        socket.close();
                                    }catch (Exception e){

                                    }
                                }
                            }catch (Exception e){
                                logger.warn(e.getMessage(), e);
                            }
                        }
                    }
                    // 6. first available network address
                    if (NetUtils.isInvalidLocalHost(hostToBind)){
                        hostToBind = NetUtils.getLocalHost();
                    }
                }
            }
        }

        map.put(Constants.BIND_IP_KEY, hostToBind);

        // register.
        String registryHost = getValueFromConfigurator(protocolConfig, Constants.MANDAL_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(registryHost) && NetUtils.isInvalidLocalHost(registryHost)){
            throw new IllegalArgumentException("Specified invalid registry ip from property. " + Constants.MANDAL_IP_TO_REGISTRY + ".value" + registryHost);
        }else if (StringUtils.isEmpty(registryHost)){
            registryHost = hostToBind;
        }

        map.put(Constants.ANYHOST_KEY, registryHost);

        return registryHost;
    }

    /**
     * Register port and bind port to Provider. configured separately.
     * Configuration priority: environment variable -> java system properties -> port property in protocol config file
     * -> protocol default port
     * @param protocolConfig
     * @param serviceName
     * @param map
     * @return
     */
    private Integer findConfigedPort(ProtocolConfig protocolConfig, String serviceName, Map<String, String> map) {

        Integer portToBind;

        // 1. environment variable
        String port = getValueFromConfigurator(protocolConfig, Constants.MANDAL_PORT_TO_BIND);
        portToBind = parsePort(port);

        if (Objects.isNull(portToBind)){
            // 2. java system properties
            portToBind = protocolConfig.getPort();
            // 3. port property in protocol config file
            if (!Objects.isNull(provider) && (portToBind == null || portToBind == 0)){
                portToBind = provider.getPort();
            }
            final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(serviceName).getDefaultPort();
            if (Objects.isNull(portToBind) || portToBind == 0){
                portToBind = defaultPort;
            }
            if (Objects.isNull(portToBind) || portToBind <= 0){
                portToBind = getRandomPort(serviceName);
                if (Objects.isNull(portToBind) || portToBind <= 0){
                    portToBind = NetUtils.getAvailablePort(defaultPort);
                    putRandomPort(serviceName, portToBind);
                }
                logger.warn("Use random port. port: " + portToBind + " for protocol: " + serviceName);
            }
        }

        map.put(Constants.BIND_PORT_KEY, String.valueOf(portToBind));

        String portToRegistryStr = getValueFromConfigurator(protocolConfig, Constants.MANDAL_PORT_TO_BIND);
        Integer portToRegistry = parsePort(portToRegistryStr);
        if (Objects.isNull(portToRegistry)){
            portToRegistry = portToBind;
        }

        return portToRegistry;
    }

    private Integer parsePort(String configPort) {
        Integer port = null;
        if (!StringUtils.isEmpty(configPort)){
            try {
                Integer initPort = Integer.parseInt(configPort);
                if (NetUtils.isInvalidPort(initPort)){
                    throw new IllegalArgumentException("Invalid port from env value: " + initPort);
                }
                return initPort;
            }catch (Exception e){
                throw new IllegalArgumentException("Invalid port from env value: " + configPort);
            }
        }
        return port;
    }

    private String getValueFromConfigurator(ProtocolConfig protocolConfig, String key) {
        String protocolPrefix = protocolConfig.getName() + "_";
        String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
        if (StringUtils.isEmpty(port)){
            port = ConfigUtils.getSystemProperty(key);
        }
        return port;
    }

    private void exportLocal(URL url) {
        if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())){
            URL local = URL.valueOf(url.toFullString())
                    .setProtocol(Constants.LOCAL_PROTOCOL)
                    .setHost(NetUtils.LOCALHOST)
                    .setPort(0);
            ServiceClassHolder.getInstance().pushServiceClassHolder(getServiceClass(ref));

            Exporter<?> exporter = protocol.export(proxyFactory.getInvoker(ref, (Class<T>) interfaceClass, local));
            exporters.add(exporter);
            logger.info("Export Mandal service " + interfaceClass.getName() + " to local registry.");
        }
    }

    private Class getServiceClass(T ref) {
        return ref.getClass();
    }

    public Class<?> getInterfaceClass(){
        if (!Objects.isNull(interfaceClass)){
            return interfaceClass;
        }
        if (ref instanceof GenericService){
            return GenericService.class;
        }
        try {
            if (!StringUtils.isEmpty(interfaceName)){
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return interfaceClass;
    }

    protected void checkDefault(){
        if (Objects.isNull(provider)){
            provider = new ProviderConfig();
        }
        appendProperties(provider);
    }

    public synchronized void unexport(){
        if (!export){
            return;
        }
        if (unexported){
            return;
        }
        if (!CollectionUtils.isEmpty(exporters)){
            for (Exporter<?> exporter : exporters) {
                try {
                    exporter.unexport();
                }catch (Throwable t){
                    logger.warn("Unexported error when unexport " + exporter, t);
                }
            }
        }
        unexported = true;
    }

    protected void checkRef(){
        if (Objects.isNull(ref)){
            throw new IllegalStateException("ref is null!");
        }
        if (!interfaceClass.isInstance(ref)){
            throw new IllegalStateException("Error occured the class " + ref.getClass().getName() + " unimplemented interface " + interfaceClass + "!");
        }
    }

    protected void checkProtocol(){
        if (CollectionUtils.isEmpty(protocols) && !Objects.isNull(provider)){
            setProtocols(protocols);
        }
        if (CollectionUtils.isEmpty(protocols)){
            setProtocol(new ProtocolConfig());
        }
        protocols.forEach(protocolConfig -> {
            if (StringUtils.isEmpty(protocolConfig.getName())){
                protocolConfig.setName("mandal");
            }
            appendProperties(protocolConfig);
        });
    }


}
