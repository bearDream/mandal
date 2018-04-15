package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.AbstractConfig;
import cn.ching.mandal.config.spring.beans.factory.annotation.MandalConfigBindingBeanPostProcessor;
import cn.ching.mandal.config.spring.utils.PropertySourcesUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 2018/4/7
 * Binding Bean Registra.
 * @see EnableMandalConfigBinding
 * @see cn.ching.mandal.config.spring.beans.factory.annotation.MandalConfigBindingBeanPostProcessor
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalConfigBindingRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private final Log logger = LogFactory.getLog(MandalConfigBindingRegistrar.class);

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableMandalConfigBinding.class.getName()));

        registerBeanDefinitions(attributes, registry);
    }

    protected void registerBeanDefinitions(AnnotationAttributes attributes, BeanDefinitionRegistry registry) {

        String prefix = environment.resolvePlaceholders(attributes.getString("prefix"));

        Class<? extends AbstractConfig> configClass = attributes.getClass("type");

        boolean multiple = attributes.getBoolean("multiple");

        registerMandalConfigBeans(prefix, configClass, multiple, registry);
    }

    /**
     * batch register bean.
     * @param prefix
     * @param configClass
     * @param multiple
     * @param registry
     */
    private void registerMandalConfigBeans(String prefix, Class<? extends AbstractConfig> configClass, boolean multiple, BeanDefinitionRegistry registry) {

        PropertySources propertySources = environment.getPropertySources();

        Map<String, String> properties = PropertySourcesUtils.getSubProperties(propertySources, prefix);

        if (CollectionUtils.isEmpty(properties)){
            if (logger.isDebugEnabled()){
                logger.debug("There is no property for binding to mandal config class. [" + configClass.getName() + " ] within prefix [" + prefix + "]");
            }
            return;
        }

        Set<String> beanNames = multiple ? resolveMultipleBeanName(prefix, properties) : Collections.singleton(resolveSingleBeanName(configClass, properties, registry));

        for (String beanName : beanNames) {
            registerMandalConfigBean(beanName, configClass, registry);

            MutablePropertyValues propertyValues = resolveBeanPropertyValues(beanName, multiple, properties);

            registerMandalConfigBindingBeanPostProcessor(beanName, propertyValues, registry);
        }

    }


    /**
     * single register bean.
     * @param beanName
     * @param configClass
     * @param registry
     */
    private void registerMandalConfigBean(String beanName, Class<? extends AbstractConfig> configClass, BeanDefinitionRegistry registry) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(configClass);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        registry.registerBeanDefinition(beanName, beanDefinition);

        if (logger.isInfoEnabled()){
            logger.info("The Mandal config bean definition [ name: " + beanName + ", class: " + configClass.getName() + " ] has been registerd!");
        }
    }

    private String resolveSingleBeanName(Class<? extends AbstractConfig> configClass, Map<String, String> properties, BeanDefinitionRegistry registry) {

        String beanName = properties.get("id");
        if (!StringUtils.hasText(beanName)){
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(configClass);
            beanName = BeanDefinitionReaderUtils.generateBeanName(builder.getRawBeanDefinition(), registry);
        }
        return beanName;
    }


    private Set<String> resolveMultipleBeanName(String prefix, Map<String, String> properties) {

        Set<String> beanNames = new LinkedHashSet<>();
        for (String propertyName : properties.keySet()) {

            int index = propertyName.indexOf(".");
            if (index > 0){
                String beanName = propertyName.substring(0, index);
                beanNames.add(beanName);
            }
        }
        return beanNames;

    }

    private MutablePropertyValues resolveBeanPropertyValues(String beanName, boolean multiple, Map<String, String> properties) {

        MutablePropertyValues propertyValues = new MutablePropertyValues();

        if (multiple){
            // multi Bean
            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(new MapPropertySource(beanName, new TreeMap<String, Object>(properties)));

            Map<String, String> subProperties = PropertySourcesUtils.getSubProperties(propertySources, beanName);

            propertyValues.addPropertyValues(subProperties);
        }else {
            // singleton Bean
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                if (!propertyName.contains(".")){
                    propertyValues.addPropertyValue(propertyName, entry.getValue());
                }
            }
        }
        return propertyValues;
    }

    private void registerMandalConfigBindingBeanPostProcessor(String beanName, MutablePropertyValues propertyValues, BeanDefinitionRegistry registry) {

        Class<?> processorClass = MandalConfigBindingBeanPostProcessor.class;

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(processorClass);

        builder.addConstructorArgValue(beanName).addConstructorArgValue(propertyValues);

        AbstractBeanDefinition beanDefinition = builder.getRawBeanDefinition();

        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);

        if (logger.isInfoEnabled()){
            logger.info("The BeanPostProcessor bean definition [" + processorClass.getName() + "] for mandal config bean [name : " + beanName + " ] has been registerd.");
        }

    }
}
