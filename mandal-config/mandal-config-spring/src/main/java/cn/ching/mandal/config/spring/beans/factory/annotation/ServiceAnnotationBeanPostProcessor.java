package cn.ching.mandal.config.spring.beans.factory.annotation;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.config.annoatation.Service;
import cn.ching.mandal.config.spring.ServiceBean;
import cn.ching.mandal.config.spring.context.annotation.MandalClassPathBeanDefinitionScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 2018/4/7
 * Provider Service {@link Service} annotaion.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ServiceAnnotationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<String> packageToScan;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    public ServiceAnnotationBeanPostProcessor(String... packageToScan){
        this(Arrays.asList(packageToScan));
    }

    public ServiceAnnotationBeanPostProcessor(Set<String> packageToScan){
        this.packageToScan = packageToScan;
    }

    public ServiceAnnotationBeanPostProcessor(Collection<String> packageToScan){
        this(new LinkedHashSet<>(packageToScan));
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // none.
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * register bean definition.
     * @param registry
     * @throws BeansException
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Set<String> pkgToScans = resolvePackageToScan(packageToScan);

        if (!CollectionUtils.isEmpty(pkgToScans)){
            registerServiceBeans(pkgToScans, registry);
        }else {
            if (logger.isWarnEnabled()){
                logger.warn("packageToScan is empty. ServiceBean registry will be ignored.");
            }
        }
    }

    /**
     * resolve placeholder. as like that: ${...}
     * @see PropertyResolver#resolvePlaceholders(String)
     * @param packagesToScan
     * @return
     */
    private Set<String> resolvePackageToScan(Set<String> packagesToScan) {

        Set<String> resolvedPackageToScan = new LinkedHashSet<>(packagesToScan.size());
        packagesToScan.stream()
                .filter(p -> StringUtils.hasText(p))
                .forEach(p -> {
                    String resolvePackgeToScan = environment.resolvePlaceholders(p.trim());
                    resolvedPackageToScan.add(resolvePackgeToScan);
                });
        return resolvedPackageToScan;
    }

    /**
     * register beans whose classes was annotated {@link cn.ching.mandal.config.annoatation.Service}
     * @param pkgsToScans
     * @see ClassPathBeanDefinitionScanner
     */
    private void registerServiceBeans(Set<String> pkgsToScans, BeanDefinitionRegistry registry) {

        MandalClassPathBeanDefinitionScanner scanner = new MandalClassPathBeanDefinitionScanner(registry, environment, resourceLoader);

        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);

        scanner.setBeanNameGenerator(beanNameGenerator);

        // filter with @Service annotation class.
        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));

        for (String packageScan : pkgsToScans) {

            // 1. registers @Service Bean.
            scanner.scan(packageScan);

            // 2. find all BeanDefinitionHolders of @Service whether @ComponentScan scans or not.
            Set<BeanDefinitionHolder> beanDefinitionHolders = findServiceBeanDefinitionHolders(scanner, packageScan, registry, beanNameGenerator);

            if (!CollectionUtils.isEmpty(beanDefinitionHolders)){

                // foreach register class with @Service.
                for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders) {
                    registerServiceBeans(beanDefinitionHolder, registry, scanner);
                }
                if (logger.isInfoEnabled()){
                    logger.info(beanDefinitionHolders.size() + " annotated Mandal's @Service Components { " +
                                beanDefinitionHolders + " } were scanned under package[ " + packageScan + " ]");
                }
            }else {

                if (logger.isWarnEnabled()){
                    logger.warn("No Spring bean annotation Mandal's @Service was found under package[ " + packageScan + " ]");
                }
            }

        }
        
    }

    /**
     * register {@link ServiceBean} from new annotated {@link Service} {@link BeanDefinition}.
     * In other words, transformers class with @Service to Spring's ServiceBean.
     * @param beanDefinitionHolder
     * @param registry
     * @param scanner
     */
    private void registerServiceBeans(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry, MandalClassPathBeanDefinitionScanner scanner) {

        // load beanDefinitionHolder
        Class<?> beanClass = resolveClass(beanDefinitionHolder);

        // get beanClass's @Service.
        Service service = AnnotationUtils.findAnnotation(beanClass, Service.class);

        // get beanDefinition's @Service interfaceName/interfaceClass and load it.
        Class<?> interfaceClass = resolveServiceInterfaceClass(beanClass, service);

        String annotatedServiceBeanName = beanDefinitionHolder.getBeanName();

        AbstractBeanDefinition serviceBeanDefinition = buildServiceBeanDefinition(service, interfaceClass, annotatedServiceBeanName);

        String beanName = generateServiceBeanName(interfaceClass, annotatedServiceBeanName);

        if (scanner.checkCandidate(beanName, serviceBeanDefinition)){
            // register.
            registry.registerBeanDefinition(beanName, serviceBeanDefinition);
            if (logger.isWarnEnabled()){
                logger.warn("The BeanDefinition[ " + serviceBeanDefinition + " ] of ServiceBean has been registered with name: " + beanName);
            }
        }else {
            if (logger.isWarnEnabled()){
                logger.warn("The Duplicated BeanDefinition[ " + serviceBeanDefinition + " ] of ServiceBean[ bean name: "
                            + beanName + " ] was not found, check @MandalComponentScan scan to same package in any times?");
            }
        }
    }

    private String generateServiceBeanName(Class<?> interfaceClass, String annotatedServiceBeanName) {
        return "ServiceBean@" + interfaceClass.getName() + "#" + annotatedServiceBeanName;
    }

    /**
     * Get {@link Service} attribute and config it.
     * @param annotatedServiceBeanClass
     * @param service
     * @return
     */
    private Class<?> resolveServiceInterfaceClass(Class<?> annotatedServiceBeanClass, Service service) {

        Class<?> interfaceClass = service.interfaceClass();

        if (void.class.equals(interfaceClass)){

            interfaceClass = null;
            String interfaceName = service.interfaceName();

            if (StringUtils.hasText(interfaceName) && ClassUtils.isPresent(interfaceName, classLoader)){
                interfaceClass = ClassUtils.resolveClassName(interfaceName, classLoader);
            }
        }

        if (Objects.isNull(interfaceClass)){

            Class<?>[] allInterfaces = annotatedServiceBeanClass.getInterfaces();

            if (allInterfaces.length > 0){
                interfaceClass = allInterfaces[0];
            }
        }

        Assert.notNull(interfaceClass, "@Service interfaceClass() or interfaceName() or interface class must present!");
        Assert.isTrue(interfaceClass.isInterface(), "@Service interfaceClass() is not an interface!");

        return interfaceClass;
    }

    /**
     * according to Service attribute build BeanDefinition.
     * @param service
     * @param interfaceClass
     * @param annotatedServiceBeanName
     * @return
     */
    private AbstractBeanDefinition buildServiceBeanDefinition(Service service, Class<?> interfaceClass, String annotatedServiceBeanName) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .rootBeanDefinition(ServiceBean.class)
                .addConstructorArgValue(service)
                .addPropertyReference("ref", annotatedServiceBeanName)
                .addPropertyValue("interface", interfaceClass.getName());

        // ProviderConfig
        String providerConfigBeanName = service.provider();
        if (StringUtils.hasText(providerConfigBeanName)){
            addPropertyReference(builder, "provider", providerConfigBeanName);
        }

        // MonitorConfig
        String MonitorConfigName = service.monitor();
        if (StringUtils.hasText(MonitorConfigName)){
            addPropertyReference(builder, "monitor", MonitorConfigName);
        }

        // ApplicationConfig
        String ApplicationConfigName = service.application();
        if (StringUtils.hasText(ApplicationConfigName)){
            addPropertyReference(builder, "application", ApplicationConfigName);
        }

        // ModuleConfig
        String ModuleConfigName = service.module();
        if (StringUtils.hasText(ModuleConfigName)){
            addPropertyReference(builder, "module", ModuleConfigName);
        }

        // RegistryConfig
        String[] RegistryConfigName = service.registry();
        List<RuntimeBeanReference> registryRuntimeBeanReference = toRuntimeBeanReference(RegistryConfigName);

        if (!registryRuntimeBeanReference.isEmpty()){
            builder.addPropertyValue( "registries", registryRuntimeBeanReference);
        }

        // ProtocolConfig
        String[] protocolConfigName = service.protocol();
        List<RuntimeBeanReference> protocolRuntimeBeanReference = toRuntimeBeanReference(protocolConfigName);

        if (!protocolRuntimeBeanReference.isEmpty()){
            builder.addPropertyValue("protocols", protocolRuntimeBeanReference);
        }

        return builder.getBeanDefinition();
    }

    // build ManagedList<RuntimeBeanReference>
    private List<RuntimeBeanReference> toRuntimeBeanReference(String... beanNames) {

        ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();

        if (!ObjectUtils.isEmpty(beanNames)){
            for (String beanName : beanNames) {
                String resolvedBeanName = environment.resolvePlaceholders(beanName);
                runtimeBeanReferences.add(new RuntimeBeanReference(resolvedBeanName));
            }
        }
        return runtimeBeanReferences;
    }

    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String beanName) {
        String resolvedName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedName);
    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {

        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(beanClassName, classLoader);
    }


    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {

        BeanNameGenerator beanNameGenerator = null;

        if (registry instanceof SingletonBeanRegistry){
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        if (Objects.isNull(beanNameGenerator)){
            if (logger.isInfoEnabled()){
                logger.info("BeanNameGenerator bean can't be found in BeanFactory with name [ " + AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR + " ]");
                logger.info("BeanNameGenerator will be a instance of " + AnnotationBeanNameGenerator.class.getName() + ", it's maybe a potential problem on bean name generation.");
            }

            beanNameGenerator = new AnnotationBeanNameGenerator();
        }

        return beanNameGenerator;
    }

    private Set<BeanDefinitionHolder> findServiceBeanDefinitionHolders(ClassPathBeanDefinitionScanner scanner, String packageScan, BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {

        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageScan);

        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(beanDefinitions.size());

        for (BeanDefinition beanDefinition : beanDefinitions) {

            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        }
        return beanDefinitionHolders;
    }
}
