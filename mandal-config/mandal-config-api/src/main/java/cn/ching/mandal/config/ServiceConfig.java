package cn.ching.mandal.config;

import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.ClassHelper;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.annoatation.Service;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.ProxyFactory;
import cn.ching.mandal.rpc.service.GenericService;
import lombok.Getter;
import lombok.Setter;

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

    //todo
    private void doExportUrls() {

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
