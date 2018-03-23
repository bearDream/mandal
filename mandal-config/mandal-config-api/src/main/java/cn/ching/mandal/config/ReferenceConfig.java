package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.bytecode.Wrapper;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.config.model.ApplicationModel;
import cn.ching.mandal.config.model.ConsumerModel;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.ProxyFactory;
import cn.ching.mandal.rpc.StaticContext;
import cn.ching.mandal.rpc.cluster.Cluster;
import cn.ching.mandal.rpc.service.GenericService;
import cn.ching.mandal.rpc.support.ProtocolUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private static final Protocol reprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

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
    private String protocol;
    // interface proxy reference
    private transient volatile T ref;
    private transient volatile Invoker<T> invoker;
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

    public ReferenceConfig(){

    }

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
        }else if (isInvalidLocalHost(hostToRegistry)){
            throw new IllegalArgumentException("specified invalid registry ip from property:" + Constants.MANDAL_IP_TO_REGISTRY + ".value: " + hostToRegistry);
        }
        map.put(Constants.REGISTRY_KEY, hostToRegistry);

        StaticContext.getSystemContext().putAll(attributes);
        ref = createProxy(map);
        ConsumerModel consumerModel = new ConsumerModel();
        ApplicationModel.initConsumerModel(getUniqueServiceName(), consumerModel);


    }

    private boolean isInvalidLocalHost(String hostToRegistry) {
    }

    private void checkAndConvertImplicitConfig(MethodConfig method, Map<String, String> map, Map<Object, Object> attributes) {
    }

    private void checkDefault() {
    }

}
