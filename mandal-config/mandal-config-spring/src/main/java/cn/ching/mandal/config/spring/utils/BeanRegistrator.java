package cn.ching.mandal.config.spring.utils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Set;

/**
 * 2018/4/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class BeanRegistrator {

    public static void registerInfrastructureBean(BeanDefinitionRegistry beanDefinitionRegistry, String beanName, Class<?> beanType){

        if (!beanDefinitionRegistry.containsBeanDefinition(beanName)){
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanType);
            builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
            beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    public static void registerInfrastructureBeanWithArg(BeanDefinitionRegistry registry, Set<String> constructArg, Class<?> beanType){

        // build a BeanDefinitionBuilder with ServiceAnnotationBeanPostProcessor.
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanType);
        builder.addConstructorArgValue(constructArg);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        // according to bean definition and bean factory register this bean.
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }
}
