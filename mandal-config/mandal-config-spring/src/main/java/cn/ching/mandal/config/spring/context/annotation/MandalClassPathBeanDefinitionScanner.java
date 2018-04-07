package cn.ching.mandal.config.spring.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.util.Set;

/**
 * 2018/4/7
 * Mandal {@link ClassPathBeanDefinitionScanner} that exposed some methods to public.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner{

    public MandalClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry,
                                                boolean useDefaultFilters,
                                                Environment environment,
                                                ResourceLoader resourceLoader) {
        super(registry, useDefaultFilters);

        setEnvironment(environment);

        setResourceLoader(resourceLoader);

        AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
    }

    public MandalClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry,
                                                Environment environment,
                                                ResourceLoader resourceLoader) {
        super(registry, false, environment, resourceLoader);
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }

    @Override
    public boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        return super.checkCandidate(beanName, beanDefinition);
    }
}
