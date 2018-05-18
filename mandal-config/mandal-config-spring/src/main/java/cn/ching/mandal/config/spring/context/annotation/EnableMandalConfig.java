package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.*;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 2018/4/7
 * As  a convenient and multiple {@link EnableMandalConfigBinding}
 * in default behavior , is equal to single bean bindings with below convention prefixes of properties:
 * <ul>
 * <li>{@link ApplicationConfig} binding to property : "mandal.application"</li>
 * <li>{@link ModuleConfig} binding to property :  "mandal.module"</li>
 * <li>{@link RegistryConfig} binding to property :  "mandal.registry"</li>
 * <li>{@link ProtocolConfig} binding to property :  "mandal.protocol"</li>
 * <li>{@link MonitorConfig} binding to property :  "mandal.monitor"</li>
 * <li>{@link ProviderConfig} binding to property :  "mandal.provider"</li>
 * <li>{@link ConsumerConfig} binding to property :  "mandal.consumer"</li>
 * </ul>
 * <p>
 * In contrast, on multiple bean bindings that requires to set {@link #multiple()} to be <code>true</code> :
 * <ul>
 * <li>{@link ApplicationConfig} binding to property : "mandal.applications"</li>
 * <li>{@link ModuleConfig} binding to property :  "mandal.modules"</li>
 * <li>{@link RegistryConfig} binding to property :  "mandal.registries"</li>
 * <li>{@link ProtocolConfig} binding to property :  "mandal.protocols"</li>
 * <li>{@link MonitorConfig} binding to property :  "mandal.monitors"</li>
 * <li>{@link ProviderConfig} binding to property :  "mandal.providers"</li>
 * <li>{@link ConsumerConfig} binding to property :  "mandal.consumers"</li>
 * </ul>
 * @author chi.zhang.
 * @email laxzhang@outlook.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(MandalConfigConfigurationSelector.class)
public @interface EnableMandalConfig {

    /**
     * whether binding to multiple Spring beans.
     * @return
     */
    boolean multiple() default false;
}
