package cn.ching.mandal.config.spring.configuration;

import cn.ching.mandal.config.AbstractConfig;
import cn.ching.mandal.config.spring.beans.factory.annotation.ReferenceAnnotaionBeanPostProcessor;
import cn.ching.mandal.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;
import cn.ching.mandal.config.spring.context.annotation.EnableMandalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Set;

import static cn.ching.mandal.config.spring.utils.MandalUtils.BASE_PACKAGES_PROPERTY_NAME;
import static cn.ching.mandal.config.spring.utils.MandalUtils.MULTIPLE_CONFIG_PROPERTY_NAME;
import static cn.ching.mandal.config.spring.utils.MandalUtils.Mandal_PREFIX;

/**
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: Mandal Auto Config{@link org.springframework.context.annotation.Configuration}
 * config mandal properties. deal with without values.
 **/
@Configuration
@ConditionalOnProperty(prefix = Mandal_PREFIX, name = "enabled", matchIfMissing = true, havingValue = "true")
@ConditionalOnClass(AbstractConfig.class)
@EnableConfigurationProperties(value = {MandalScanProperties.class, MandalConfigProperties.class})
public class MandalAutoConfiguration {

    /**
     * single Mandal config Configuration
     */
    @ConditionalOnProperty(name = MULTIPLE_CONFIG_PROPERTY_NAME, havingValue = "false", matchIfMissing = true)
    @EnableMandalConfig
    @EnableConfigurationProperties(SingleMandalConfigConfiguration.class)
    protected static class SingleMandalConfigConfiguration{

    }

    /**
     * multiple Mandal config Configuration
     * {@link EnableMandalConfig#multiple()} is true.
     */
    @ConditionalOnProperty(name = MULTIPLE_CONFIG_PROPERTY_NAME, havingValue = "true")
    @EnableMandalConfig
    @EnableConfigurationProperties(MultipleMandalConfiguration.class)
    protected static class MultipleMandalConfiguration{

    }

    /**
     * create {@link ServiceAnnotationBeanPostProcessor} if absent.
     * @param environment
     * @return
     */
    @ConditionalOnProperty(name = BASE_PACKAGES_PROPERTY_NAME)
    @Autowired
    @Bean
    public static ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor(Environment environment){
        RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment);
        Set<String> pkgScans = resolver.getProperty(BASE_PACKAGES_PROPERTY_NAME, Set.class, Collections.emptySet());
        return new ServiceAnnotationBeanPostProcessor(pkgScans);
    }

    /**
     * create {@link ReferenceAnnotaionBeanPostProcessor} if absent.
     * @param environment
     * @return
     */
    @ConditionalOnMissingBean
    @Bean(name = ReferenceAnnotaionBeanPostProcessor.BEAN_NAME)
    public static ReferenceAnnotaionBeanPostProcessor referenceAnnotaionBeanPostProcessor(Environment environment){
        return new ReferenceAnnotaionBeanPostProcessor();
    }
}
