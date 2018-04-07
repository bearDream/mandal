package cn.ching.mandal.config.spring.beans.factory.annotation;

import cn.ching.mandal.config.spring.ReferenceBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.PriorityOrdered;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private final ConcurrentMap<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>();

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

        // todo
        return null;
    }

    public Collection<ReferenceBean<?>> getReferenceBean() {
        return referenceBeansCache.values();
    }

    private InjectionMetadata findReferenceMetaData(String beanName, Class<?> beanType, Object o) {

        // todo
        return null;
    }
}
