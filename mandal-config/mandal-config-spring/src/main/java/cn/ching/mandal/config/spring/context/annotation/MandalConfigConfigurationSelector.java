package cn.ching.mandal.config.spring.context.annotation;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;

/**
 * 2018/4/7
 * todo
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalConfigConfigurationSelector implements ImportSelector, Order {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[0];
    }

    @Override
    public int value() {
        return 0;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
