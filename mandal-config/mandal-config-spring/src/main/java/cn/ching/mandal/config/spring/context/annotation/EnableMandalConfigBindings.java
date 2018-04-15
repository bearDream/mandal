package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.AbstractConfig;
import cn.ching.mandal.config.ApplicationConfig;
import cn.ching.mandal.config.ModuleConfig;
import cn.ching.mandal.config.RegistryConfig;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.PropertySource;

import java.lang.annotation.*;

/**
 * 2018/4/7
 *
 * Multiple {@link EnableMandalConfigBinding} {@link Annotation}
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MandalConfigBindingRegistrar.class)
public @interface EnableMandalConfigBindings {

    /**
     * The value of {@link EnableMandalConfigBinding}
     * @return
     */
    EnableMandalConfigBinding[] value();

}
