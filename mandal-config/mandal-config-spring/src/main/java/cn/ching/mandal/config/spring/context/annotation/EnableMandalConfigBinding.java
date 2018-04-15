package cn.ching.mandal.config.spring.context.annotation;

import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertySource;
import org.springframework.context.annotation.PropertySources;
import cn.ching.mandal.config.*;

import java.lang.annotation.*;

/**
 * 2018/4/7
 *
 * Enable Spring's annotation-driven {@link AbstractConfig Mandal config} from {@link PropertySource properties}.
 * Default, {@link #prefix()} associate with a prefix of {@link PropertySources properties}, like: "mandal.application." or "mandal.application"
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MandalConfigBindingRegistrar.class)
public @interface EnableMandalConfigBinding {

    /**
     * The name prefix of the properties that are valid to bind to {@link AbstractConfig Mandal config}.
     *
     * @return the name prefix of the properties to bind.
     */
    String prefix();

    /**
     * @return the binding type of {@link AbstractConfig Mandal config}
     * @see AbstractConfig
     * @see ApplicationConfig
     * @see ModuleConfig
     * @see RegistryConfig
     */
    Class<? extends AbstractConfig> type();

    /**
     * whether {@link #prefix()} binding to multiple Springs bean.
     * @return
     */
    boolean multiple() default false;
}
