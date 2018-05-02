package cn.ching.mandal.config.spring.beans.factory.annotation;

import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.config.spring.ReferenceBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;

/**
 * 2018/4/7
 * Consumer Service {@link cn.ching.mandal.config.annoatation.Reference} annotation.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ReferenceAnnotaionBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements MergedBeanDefinitionPostProcessor, PriorityOrdered, ApplicationContextAware,
        BeanClassLoaderAware, DisposableBean {

    // ReferenceAnnotaionBeanPostProcessor bean name.
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    private final Log logger = LogFactory.getLog(getClass());

    private ApplicationContext applicationContext;

    private ClassLoader classLoader;

    private final ConcurrentMap<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

    private final ConcurrentMap<String, ReferenceBean<?>> referenceBeansCache = new ConcurrentHashMap<>();

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void destroy() throws Exception {
        for (ReferenceBean<?> referenceBean : referenceBeansCache.values()) {
            if (logger.isInfoEnabled()){
                logger.info(referenceBean + " was destroyed!");
            }
            referenceBean.destroy();
        }

        injectionMetadataCache.clear();
        referenceBeansCache.clear();

        if (logger.isInfoEnabled()){
            logger.info(getClass() + " was destroying!");
        }
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (Objects.isNull(beanType)){
            InjectionMetadata metadata = findReferenceMetaData(beanName, beanType, null);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

        InjectionMetadata metadata = findReferenceMetaData(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        }catch (BeanCreationException e){
            throw e;
        }catch (Throwable t) {
            throw new BeanCreationException(beanName, "Inject of @Reference dependencies failed.", t);
        }
        return pvs;
    }

    public Collection<ReferenceBean<?>> getReferenceBean() {
        return referenceBeansCache.values();
    }

    private InjectionMetadata findReferenceMetaData(String beanName, Class<?> clazz, PropertyValues propertyValues) {

        String cacheKey = (StringUtils.hasText(beanName)) ? beanName : clazz.getName();
        InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        // 单例创建metadata
        if (InjectionMetadata.needsRefresh(metadata, clazz)){
            synchronized (injectionMetadataCache){
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)){
                    if (!Objects.isNull(metadata)){
                        metadata.clear(propertyValues);
                    }
                    try {
                        metadata = buildReferenceMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    }catch (NoClassDefFoundError e){
                        throw new IllegalArgumentException("Faild to introspec bean class [" + clazz.getName() + "] for reference metadata: could not find class that it depends on. ", e);
                    }
                }
            }
        }
        return metadata;
    }

    private InjectionMetadata buildReferenceMetadata(Class<?> beanClass) {
        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();

        elements.addAll(findFieldReferenceMetaData(beanClass));
        elements.addAll(findMethodReferenceMetaData(beanClass));

        return new InjectionMetadata(beanClass, elements);
    }

    /**
     * get fields by {@link cn.ching.mandal.config.annoatation.Reference} fields
     * @param beanClass
     * @return
     */
    private List<InjectionMetadata.InjectedElement> findFieldReferenceMetaData(Class<?> beanClass) {

        final List<InjectionMetadata.InjectedElement> elements = new LinkedList<>();

        ReflectionUtils.doWithFields(beanClass, (filed) -> {

            Reference reference = getAnnotation(filed, Reference.class);
            if (!Objects.isNull(reference)){

                if (Modifier.isStatic(filed.getModifiers())){
                    if (logger.isWarnEnabled()){
                        logger.warn("@Reference not supported annotated in static class.");
                    }
                    return;
                }
                elements.add(new ReferenceFieldElement(filed, reference));
            }
        });

        return elements;
    }

    /**
     * get method by {@link cn.ching.mandal.config.annoatation.Reference} methods.
     * @param beanClass
     * @return
     */
    private List<InjectionMetadata.InjectedElement> findMethodReferenceMetaData(Class<?> beanClass) {

        final List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();

        ReflectionUtils.doWithMethods(beanClass, (method) -> {

            Method bridgeMethod = BridgeMethodResolver.findBridgedMethod(method);

            if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgeMethod)){
                return;
            }

            Reference reference = findAnnotation(bridgeMethod, Reference.class);

            Optional.ofNullable(reference)
                    .ifPresent(r -> {
                        if (method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))){
                            if (Modifier.isStatic(method.getModifiers())){
                                if (logger.isWarnEnabled()){
                                    logger.warn("@Reference annotation is not supported on static methods.");
                                }
                                return;
                            }
                            if (method.getParameterTypes().length == 0){
                                if (logger.isWarnEnabled()){
                                    logger.warn("@Reference annotation should be used on methods with parameters: " + method);
                                }
                            }
                            PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgeMethod, beanClass);
                            elements.add(new ReferenceMethodElement(method, pd, reference));
                        }
                    });
        });

        return elements;
    }

    private class ReferenceMethodElement extends InjectionMetadata.InjectedElement{

        private final Method method;

        private final Reference reference;

        protected ReferenceMethodElement(Method method, PropertyDescriptor pd, Reference reference){
            super(method, pd);
            this.method = method;
            this.reference = reference;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            // pd source by InjectionMetadata.InjectedElement field
            Class<?> referenceClass = pd.getPropertyType();
            Object referenceBean = buildReferenceBean(reference, referenceClass);
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean, referenceBean);
        }
    }

    /**
     * @see Reference
     * @see Field
     * @see InjectionMetadata.InjectedElement
     */
    private class ReferenceFieldElement extends InjectionMetadata.InjectedElement{

        private final Field field;

        private final Reference reference;

        protected ReferenceFieldElement(Field field, Reference reference) {
            super(field, null);
            this.field = field;
            this.reference = reference;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> referenceClass = field.getType();

            Object referenceBean = buildReferenceBean(reference, referenceClass);

            ReflectionUtils.makeAccessible(field);
            field.set(bean, referenceBean);
        }
    }

    private Object buildReferenceBean(Reference reference, Class<?> referenceClass) throws Exception {

        String referenceBeanCacheKey = generateReferenceBeanCacheKey(reference, referenceClass);

        ReferenceBean<?> referenceBean = referenceBeansCache.get(referenceBeanCacheKey);

        if (Objects.isNull(referenceBean)){

            ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder.create(reference, classLoader, applicationContext).interfaceClass(referenceClass);

            referenceBean = beanBuilder.build();
            referenceBeansCache.putIfAbsent(referenceBeanCacheKey, referenceBean);
        }

        return referenceBean.get();
    }

    /**
     * generate a Reference bean cache key.
     * @param reference
     * @param beanClass
     * @return
     */
    private String generateReferenceBeanCacheKey(Reference reference, Class<?> beanClass) {

        String interfaceName = resolveInterfaceName(reference, beanClass);

        String key = reference.group() + "/" + interfaceName + ":" + reference.version();

        return key;
    }

    private String resolveInterfaceName(Reference reference, Class<?> beanClass) throws IllegalStateException{

        String interfaceName;
        if (!"".equals(reference.interfaceName())){
            interfaceName = reference.interfaceName();
        }else if (!void.class.equals(reference.interfaceClass())){
            interfaceName = reference.interfaceClass().getName();
        }else if (beanClass.isInterface()){
            interfaceName = beanClass.getName();
        }else {
            throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type " + beanClass.getName() + " is not a interface");
        }

        return interfaceName;
    }
}
