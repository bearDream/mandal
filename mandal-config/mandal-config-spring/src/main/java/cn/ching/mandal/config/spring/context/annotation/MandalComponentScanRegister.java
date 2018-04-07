package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.spring.beans.factory.annotation.ReferenceAnnotaionBeanPostProcessor;
import cn.ching.mandal.config.spring.utils.BeanRegistrator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import cn.ching.mandal.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 2018/4/7
 *
 * Mandal {@link MandalComponentScan} Bean Register.
 *
 * @see cn.ching.mandal.config.annoatation.Service
 * @see MandalComponentScan
 * @see ImportBeanDefinitionRegistrar
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalComponentScanRegister implements ImportBeanDefinitionRegistrar{

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        Set<String> packageToScan = getPackageToScan(importingClassMetadata);

        registerServiceAnnotationBeanPostProcessor(packageToScan, registry);

        registerReferenceAnnotationBeanPostProcessor(registry);
    }

    /**
     * registers {@link ServiceAnnotationBeanPostProcessor}
     * @param packageToScan package name list.
     * @param registry
     */
    private void registerServiceAnnotationBeanPostProcessor(Set<String> packageToScan, BeanDefinitionRegistry registry) {

        BeanRegistrator.registerInfrastructureBeanWithArg(registry, packageToScan, ServiceAnnotationBeanPostProcessor.class);
    }

    private void registerReferenceAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {

        // register with @Reference Annotation Bean Processor.
        BeanRegistrator.registerInfrastructureBean(registry, ReferenceAnnotaionBeanPostProcessor.BEAN_NAME, ReferenceAnnotaionBeanPostProcessor.class);

    }

    private Set<String> getPackageToScan(AnnotationMetadata metadata) {
        // 1. get Annotation attributes.
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(MandalComponentScan.class.getName()));

        // 2. get MandalComponentScan all attributes.
        String[] basePackges = attributes.getStringArray("basePackage");
        Class<?>[] basePackgesClasses = attributes.getClassArray("basePackageClasses");
        String value = value = attributes.getString("value");

        // 3. appends attributes to pkgToScan
        Set<String> pkgToScan = new LinkedHashSet<>(Arrays.asList(value));
        pkgToScan.addAll(Arrays.asList(basePackges));
        for (Class<?> basePackgesClass : basePackgesClasses) {
            pkgToScan.add(ClassUtils.getPackageName(basePackgesClass));
        }
        return pkgToScan;
    }
}
