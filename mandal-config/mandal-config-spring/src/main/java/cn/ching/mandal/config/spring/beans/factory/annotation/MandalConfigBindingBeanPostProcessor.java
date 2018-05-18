package cn.ching.mandal.config.spring.beans.factory.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.validation.DataBinder;

import java.util.Arrays;

/**
 * 2018/4/15
 *
 * Mandal config binding {@link BeanPostProcessor}
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalConfigBindingBeanPostProcessor implements BeanPostProcessor{

    private final Log logger = LogFactory.getLog(MandalConfigBindingBeanPostProcessor.class);

    /**
     * binding bean name.
     */
    private final String beanName;

    /**
     * bingding properties.
     */
    private final PropertyValues propertyValues;

    public MandalConfigBindingBeanPostProcessor(String beanName, PropertyValues propertyValues){
        Assert.notNull(beanName, "The bean name must not be null.");
        Assert.notNull(propertyValues, "The propertyValues must not be null.");
        this.beanName = beanName;
        this.propertyValues = propertyValues;
    }

    /**
     * binding properties values to bean.
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals(this.beanName)){
            DataBinder dataBinder = new DataBinder(bean);
            dataBinder.setIgnoreInvalidFields(true);
            dataBinder.bind(propertyValues);
            if (logger.isInfoEnabled()){
                logger.info("The Properties of bean [name=" + beanName + "] have been bindings by values.: " + Arrays.asList(propertyValues.getPropertyValues()));
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
