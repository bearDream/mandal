package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.*;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.monitor.api.Monitor;
import cn.ching.mandal.monitor.api.MonitorFactory;
import cn.ching.mandal.monitor.api.MonitorService;
import cn.ching.mandal.registry.RegistryFactory;
import cn.ching.mandal.registry.RegistryService;
import cn.ching.mandal.rpc.Filter;
import cn.ching.mandal.rpc.ProxyFactory;
import cn.ching.mandal.rpc.cluster.Cluster;
import cn.ching.mandal.rpc.support.MockInvoker;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AbstractInterfaceConfig extends AbstractMethodConfig {

    private static final long serialVersionUID = -1240302678663586702L;

    // local impl class name for the service interface
    protected String local;

    protected Monitor mo;
    // local stub class name for the service interface
    @Getter
    protected String stub;

    // service monitor
    @Getter
    @Setter
    protected MonitorConfig monitor;

    // proxy type
    @Getter
    protected String proxy;

    // cluster type
    @Getter
    protected String cluster;

    // filter
    protected String filter;

    @Setter
    protected String listener;

    @Getter
    protected String owner;

    // connection limit. 0 means shared connections, otherwise it defines the connections delegated to the current service.
    @Getter
    @Setter
    protected Integer connections;

    @Getter
    protected String layer;

    @Getter
    @Setter
    protected ApplicationConfig application;

    @Getter
    @Setter
    protected ModuleConfig module;

    @Getter
    protected List<RegistryConfig> registries;

    // connection events
    @Getter
    @Setter
    protected String onconnect;

    // diconnection events
    @Getter
    @Setter
    protected String ondisconnect;

    // callback limits
    @Getter
    @Setter
    private Integer callbacks;

    // the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
    @Getter
    @Setter
    private String scopes;

    protected void checkRegistry(){
        if (CollectionUtils.isEmpty(registries)){
            String address = ConfigUtils.getProperty("mandal.registry.address");
            if (!StringUtils.isEmpty(address)){
                registries = new ArrayList<>();
                // 0.0.0.0|10.1.1.0 -> 0.0.0.0 10.1.1.0
                String[] add = address.split("\\s*[|]+\\s*");
                for (String s : add) {
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.setAddress(s);
                    registries.add(registryConfig);
                }
            }
        }
        if (CollectionUtils.isEmpty(registries)){
            throw new IllegalStateException((getClass().getSimpleName().startsWith("Reference"))
                ? "No such any registries to refer service in consumer."
                : "No such any registries to export service in provider"
                + NetUtils.getLocalHost()
                + " use mandal version"
                + Version.getVersion()
                + ", please add <mandal:registry address=\"...\" /> to your spring config file. If you want unregister, please set <mandal:service registry=\"N/A\" />");
        }
        registries.forEach(registry -> appendProperties(registry));
    }

    protected void checkApplication(){
        if (Objects.isNull(application)){
            String appName = ConfigUtils.getProperty("mandal.application.name");
            if (!StringUtils.isEmpty(appName)){
                application = new ApplicationConfig();
            }
        }
        if (Objects.isNull(application)){
            throw new IllegalStateException("No such application config! please add <mandal:application name=\"...\" /> to your spring config");
        }
        appendProperties(application);

        String wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_KEY);
        if (!StringUtils.isTrimEmpty(wait)){
            System.setProperty(Constants.SHUTDOWN_WAIT_KEY, wait.trim());
        }else {
            wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
            if (!StringUtils.isTrimEmpty(wait)){
                System.setProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY, wait.trim());
            }
        }
    }

    protected List<URL> loadRegistries(boolean provider){
        checkRegistry();
        List<URL> registryList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(registries)){
            for (RegistryConfig registry : registries) {
                String address = registry.getAddress();
                if (StringUtils.isEmpty(address)){
                    address = Constants.ANYHOST_VALUE;
                }
                String sysaddress = System.getProperty("mandal.registry.address");
                if (!Objects.isNull(sysaddress)){
                    address = sysaddress;
                }
                if (!Objects.isNull(sysaddress)
                        && sysaddress.length() > 0
                        && !RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)){
                    Map<String, String> map = new HashMap<>();
                    appendParameters(map, application);
                    appendParameters(map, registry);
                    map.put("patch", RegistryService.class.getName());
                    map.put("mandal", Version.getVersion());
                    map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
                    if (ConfigUtils.getPID() > 0){
                        map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPID()));
                    }
                    if (!map.containsKey("protocol")){
                        if (ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension("remote")){
                            map.put("protocol", "remote");
                        }else {
                            map.put("protocol", "mandal");
                        }
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);
                    for (URL url : urls) {
                        url = url.setProtocol(Constants.REGISTRY_PROTOCOL)
                                .addParameter(Constants.REGISTRY_KEY, url.getProtocol());
                        if ((provider && url.getParameter(Constants.REGISTER_KEY, true))
                                || (!provider && url.getParameter(Constants.SUBSCRIBE_KEY, true))){
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        return registryList;
    }

    protected URL loadMonitor(URL registryURL){
        if (Objects.isNull(monitor)){
            String address = ConfigUtils.getProperty("mandal.monitor.address");
            String protocol = ConfigUtils.getProperty("mandal.monitor.protocol");
            if (StringUtils.isEmpty(address) && StringUtils.isEmpty(protocol)){
                return null;
            }

            monitor = new MonitorConfig();
            if (!StringUtils.isEmpty(address)){
                monitor.setAddress(address);
            }
            if (!StringUtils.isEmpty(protocol)){
                monitor.setProtocol(protocol);
            }
        }
        appendProperties(monitor);
        Map<String, String> map = new HashMap<>();
        map.put(Constants.INTERFACE_KEY, MonitorService.class.getName());
        map.put("mandal", Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPID() > 0){
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPID()));
        }
        appendParameters(map, monitor);
        String address = monitor.getAddress();
        String sysaddress = System.getProperty("mandal.monitor.address");
        if (!Objects.isNull(sysaddress) && sysaddress.length() > 0){
            address = sysaddress;
        }
        if (ConfigUtils.isNotEmpty(address)){
            if (!map.containsKey(Constants.PROTOCOL_KEY)){
                if (ExtensionLoader.getExtensionLoader(MonitorFactory.class).hasExtension("logstat")){
                    map.put(Constants.PROTOCOL_KEY, "logstat");
                }else {
                    map.put(Constants.PROTOCOL_KEY, "mandal");
                }
            }
            return UrlUtils.parseURL(address, map);
        }else if (Constants.REGISTRY_PROTOCOL.equals(monitor.getProtocol()) && !Objects.isNull(registryURL)){
            return registryURL.setProtocol("mandal")
                    .addParameter(Constants.PROTOCOL_KEY, "registry")
                    .addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map));
        }
        return null;
    }

    protected void checkInterfaceAndMethods(Class<?> interfaceCls, List<MethodConfig> methods){
        if (Objects.isNull(interfaceCls)){
            throw new IllegalStateException("interface not allow null");
        }
        if (!interfaceCls.isInterface()){
            throw new IllegalStateException("The interface class " + interfaceCls + " is not a interface");
        }
        if (!CollectionUtils.isEmpty(methods)){
            for (MethodConfig method : methods) {
                String methodName = method.getName();
                if (StringUtils.isEmpty(methodName)){
                    throw new IllegalStateException("<mandal:method> name attribute is required. The error may occured <mandal:service interface=\"" + interfaceCls.getName() + "\"... ><mandal:method name=\"\" ... /></mandal:reference>" );
                }
                boolean hasMethod = false;
                for (Method m : interfaceCls.getMethods()) {
                    if (m.getName().equals(methodName)){
                        hasMethod = true;
                        break;
                    }
                }
                if (!hasMethod){
                    throw new IllegalStateException("The interface " + interfaceCls.getName() + " not found method " + methodName);
                }
            }
        }
    }

    protected void checkStubAndMock(Class<?> interfaceCls){
        if (ConfigUtils.isNotEmpty(local)){
            Class<?> localClass = ConfigUtils.isDefault(local) ? ReflectUtils.forName(interfaceCls.getName() + "Local") : ReflectUtils.forName(local);
            if (!interfaceCls.isAssignableFrom(localClass)){
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceCls.getName());
            }
            try {
                ReflectUtils.findConstructor(localClass, interfaceCls);
            }catch (NoSuchMethodException e){
                throw new IllegalStateException("No such constructor \" public " + localClass.getName() + "(" + interfaceCls.getName() + ")\" in local implementation class " + localClass.getName());
            }
        }
        if (ConfigUtils.isNotEmpty(stub)){
            Class<?> localClass = ConfigUtils.isDefault(stub) ? ReflectUtils.forName(interfaceCls.getName() + "Stub") : ReflectUtils.forName(stub);
            if (!interfaceCls.isAssignableFrom(localClass)){
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceCls.getName());
            }
            try {
                ReflectUtils.findConstructor(localClass, interfaceCls);
            }catch (NoSuchMethodException e){
                throw new IllegalStateException("No such constructor \" public " + localClass.getSimpleName() + "(" + interfaceCls.getName() + ")\" in local implementation class " + localClass.getName());
            }
        }
        if (ConfigUtils.isNotEmpty(mock)){
            if ("mock".startsWith(Constants.RETURN_PREFIX)){
                String val = mock.substring(Constants.RETURN_PREFIX.length());
                try {
                    MockInvoker.parseMockValue(val);
                }catch (Exception e){
                    throw new IllegalStateException("Illegal mock json value in <mandal:service ... mock=\" " + mock + "\" />");
                }
            }else {
                Class<?> mockClass = ConfigUtils.isDefault(mock) ? ReflectUtils.forName(interfaceCls.getName() + "Mock") : ReflectUtils.forName(mock);
                if (!interfaceCls.isAssignableFrom(mockClass)){
                    throw new IllegalStateException("The mock implementation class " + mockClass.getName() + " not implement interface " + interfaceCls.getName());
                }
                try {
                    mockClass.getConstructor(new Class<?>[0]);
                }catch (NoSuchMethodException e){
                    throw new IllegalStateException("No such empty consutructor \"public " + mockClass.getSimpleName() + "()\" in mock implementation class" + mockClass.getName());
                }
            }
        }
    }

    public void setStub(String stub) {
        checkName("stub", stub);
        this.stub = stub;
    }

    public void setCluster(String cluster) {
        checkExtension(Cluster.class, "cluster", cluster);
        this.cluster = cluster;
    }

    public void setProxy(String proxy) {
        checkExtension(ProxyFactory.class, "proxy", proxy);
        this.proxy = proxy;
    }

    @Parameter(key = Constants.REFERENCE_FILTER_KEY, append = true)
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        checkMultiExtension(Filter.class, "filter", filter);
        this.filter = filter;
    }

    @Parameter(key = Constants.INVOKER_LISTENER_KEY, append = true)
    public String getListener() {
        return listener;
    }

    public void setLayer(String layer) {
        checkNameHasSymbol("layer", layer);
        this.layer = layer;
    }

    public RegistryConfig getRegistry(){
        return CollectionUtils.isEmpty(registries) ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<>();
        registries.add(registry);
        this.registries = registries;
    }

    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    public void setMonitor(String monitor){
        this.monitor = new MonitorConfig(monitor);
    }

    public void setMonitor(MonitorConfig monitor){
        this.monitor = monitor;
    }

    public void setOwner(String owner) {
        checkMultiName("owner", owner);
        this.owner = owner;
    }
}
