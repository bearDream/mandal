package cn.ching.mandal.config.spring.beans.factory.annotation;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.config.ConsumerConfig;
import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.config.spring.ReferenceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;

import static cn.ching.mandal.config.spring.utils.BeanFactoryUtils.getOptionalBean;

/**
 * 2018/4/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
class ReferenceBeanBuilder extends AbstractAnnotationConfigBeanBuilder<Reference, ReferenceBean>{


    protected ReferenceBeanBuilder(Reference annotation, ClassLoader classLoader, ApplicationContext applicationContext) {
        super(annotation, classLoader, applicationContext);
    }

    @Override
    protected ReferenceBean doBuild() {
        return new ReferenceBean<Object>(annotation);
    }

    @Override
    protected void preConfigureBean(Reference annotation, ReferenceBean bean) throws Exception {
        Assert.notNull(interfaceClass, "The interfaceClass must set first!");
    }

    @Override
    protected void postConfigureBean(Reference reference, ReferenceBean bean) throws Exception {

        bean.setApplicationContext(applicationContext);

        configureInterface(annotation, bean);

        configureConsumerConfig(annotation, bean);

        bean.afterPropertiesSet();
    }

    /**
     * get consumer configure by Spring.
     * @param reference
     * @param bean
     */
    private void configureConsumerConfig(Reference reference, ReferenceBean bean) {

        String consumerBeanName = reference.consumer();

        ConsumerConfig consumerConfig = getOptionalBean(applicationContext, consumerBeanName, ConsumerConfig.class);

        bean.setConsumer(consumerConfig);
    }

    /**
     * get interfaceClass by {@link Reference#interfaceClass()}
     * @param reference
     * @param bean
     */
    private void configureInterface(Reference reference, ReferenceBean bean) {

        Class<?> interfaceClass = reference.interfaceClass();

        if (void.class.equals(interfaceClass)){

            interfaceClass = null;

            String interfaceClassName = reference.interfaceName();

            if (StringUtils.hasText(interfaceClassName)){
                if (ClassUtils.isPresent(interfaceClassName, classLoader)){
                    interfaceClass = ClassUtils.resolveClassName(interfaceClassName, classLoader);
                }
            }
        }

        if (Objects.isNull(interfaceClass)){
            interfaceClass = this.interfaceClass;
        }

        Assert.isTrue(interfaceClass.isInterface(), "The class of field or method that was annotated @Reference is not an interface!");
        bean.setInterface(interfaceClass);
    }

    @Override
    protected String[] resolveRegistryConfigBeanNames(Reference annotation) {
        return annotation.registry();
    }

    @Override
    protected String resolveMonitorConfigBeanName(Reference annotation) {
        return annotation.monitor();
    }

    @Override
    protected String resolveApplicationName(Reference annotation) {
        return annotation.application();
    }

    @Override
    protected String resolveModuleName(Reference annotation) {
        return annotation.module();
    }

    public static ReferenceBeanBuilder create(Reference annotation, ClassLoader classLoader, ApplicationContext applicationContext){
        return new ReferenceBeanBuilder(annotation, classLoader, applicationContext);
    }


}
