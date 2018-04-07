package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.*;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 2018/4/7
 * As  a convenient and multiple {@link EnableMandalConfigBinding}
 * in default behavior , is equal to single bean bindings with below convention prefixes of properties:
 * <ul>
 * <li>{@link ApplicationConfig} binding to property : "dubbo.application"</li>
 * <li>{@link ModuleConfig} binding to property :  "dubbo.module"</li>
 * <li>{@link RegistryConfig} binding to property :  "dubbo.registry"</li>
 * <li>{@link ProtocolConfig} binding to property :  "dubbo.protocol"</li>
 * <li>{@link MonitorConfig} binding to property :  "dubbo.monitor"</li>
 * <li>{@link ProviderConfig} binding to property :  "dubbo.provider"</li>
 * <li>{@link ConsumerConfig} binding to property :  "dubbo.consumer"</li>
 * </ul>
 * <p>
 * In contrast, on multiple bean bindings that requires to set {@link #multiple()} to be <code>true</code> :
 * <ul>
 * <li>{@link ApplicationConfig} binding to property : "dubbo.applications"</li>
 * <li>{@link ModuleConfig} binding to property :  "dubbo.modules"</li>
 * <li>{@link RegistryConfig} binding to property :  "dubbo.registries"</li>
 * <li>{@link ProtocolConfig} binding to property :  "dubbo.protocols"</li>
 * <li>{@link MonitorConfig} binding to property :  "dubbo.monitors"</li>
 * <li>{@link ProviderConfig} binding to property :  "dubbo.providers"</li>
 * <li>{@link ConsumerConfig} binding to property :  "dubbo.consumers"</li>
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
