package cn.ching.mandal.config.spring.beans.factory.annotation;

import cn.ching.mandal.common.utils.Assert;
import cn.ching.mandal.config.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.util.List;

import static cn.ching.mandal.config.spring.utils.BeanFactoryUtils.getBeans;
import static cn.ching.mandal.config.spring.utils.BeanFactoryUtils.getOptionalBean;

/**
 * 2018/4/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
abstract class AbstractAnnotationConfigBeanBuilder<T extends Annotation, Y extends AbstractInterfaceConfig> {

    private final Log logger = LogFactory.getLog(AbstractAnnotationConfigBeanBuilder.class);

    protected final T annotation;

    protected final ApplicationContext applicationContext;

    protected final ClassLoader classLoader;

    protected Object bean;

    protected Class<?> interfaceClass;

    protected AbstractAnnotationConfigBeanBuilder(T annotation, ClassLoader classLoader, ApplicationContext applicationContext){
        Assert.notNull(annotation, "The application must not be null!");
        Assert.notNull(classLoader, "The classLoader must not be null!");
        Assert.notNull(applicationContext, "The ApplicationContext not be null!");
        this.annotation = annotation;
        this.applicationContext = applicationContext;
        this.classLoader = classLoader;
    }

    public <O extends AbstractAnnotationConfigBeanBuilder<T, Y>> O bean(Object bean){
        this.bean = bean;
        return (O) this;
    }

    public <O extends AbstractAnnotationConfigBeanBuilder<T, Y>> O interfaceClass(Class<?> interfaceClass){
        this.interfaceClass = interfaceClass;
        return (O) this;
    }

    public final Y build() throws Exception{

        Y bean = doBuild();

        configureBean(bean);

        if (logger.isInfoEnabled()){
            logger.info(bean + "has been built!");
        }

        return bean;
    }

    protected abstract Y doBuild();

    protected void configureBean(Y bean) throws Exception{

        // Before configure
        preConfigureBean(annotation, bean);

        configureRegistryConfigs(bean);

        configureMonitorConfig(bean);

        configureApplicationConfig(bean);

        configureModuleConfig(bean);

        // after configure
        postConfigureBean(annotation, bean);

    }

    protected abstract void preConfigureBean(T annotation, Y bean) throws Exception;

    protected abstract void postConfigureBean(T annotation, Y bean) throws Exception;

    private void configureRegistryConfigs(Y bean) {

        String[] registryConfigBeanIds = resolveRegistryConfigBeanNames(annotation);

        List<RegistryConfig> registryConfigs = getBeans(applicationContext, registryConfigBeanIds, RegistryConfig.class);

        bean.setRegistries(registryConfigs);
    }

    private void configureMonitorConfig(Y bean) {

        String monitorBeanName = resolveMonitorConfigBeanName(annotation);

        MonitorConfig monitorConfig = getOptionalBean(applicationContext, monitorBeanName, MonitorConfig.class);

        bean.setMonitor(monitorConfig);
    }

    private void configureApplicationConfig(Y bean) {

        String applicationConfigureBeanName = resolveApplicationName(annotation);

        ApplicationConfig applicationConfig = getOptionalBean(applicationContext, applicationConfigureBeanName, ApplicationConfig.class);

        bean.setApplication(applicationConfig);
    }

    private void configureModuleConfig(Y bean) {

        String moduleConfigureBeanName = resolveModuleName(annotation);

        ModuleConfig moduleConfig =  getOptionalBean(applicationContext, moduleConfigureBeanName, ModuleConfig.class);

        bean.setModule(moduleConfig);
    }

    protected abstract String[] resolveRegistryConfigBeanNames(T annotation);

    protected abstract String resolveMonitorConfigBeanName(T annotation);

    protected abstract String resolveApplicationName(T annotation);

    protected abstract String resolveModuleName(T annotation);


}
