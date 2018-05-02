package cn.ching.mandal.config.spring.context.annotation;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;

/**
 * 2018/4/7
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalConfigConfigurationSelector implements ImportSelector, Ordered {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableMandalConfig.class.getName()));
        boolean multiple = attributes.getBoolean("multiple");

        if (multiple){
            return of(MandalConfigConfiguration.Multiple.class.getName());
        }else {
            return of(MandalConfigConfiguration.Single.class.getName());
        }
    }

    private static <T> T[] of(T... values){
        return values;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
