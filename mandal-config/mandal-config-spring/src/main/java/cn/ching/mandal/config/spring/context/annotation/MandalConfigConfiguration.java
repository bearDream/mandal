package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.*;

/**
 * 2018/4/16
 *
 * {@link AbstractConfig}
 * {@link org.springframework.context.annotation.Configuration}
 *
 * @see org.springframework.context.annotation.Configuration
 * @see EnableMandalConfigBinding
 * @see EnableMandalConfigBindings
 * @see ApplicationConfig
 * @see ModuleConfig
 * @see RegistryConfig
 * @see ProtocolConfig
 * @see MonitorConfig
 * @see ProviderConfig
 * @see ConsumerConfig
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalConfigConfiguration {

    @EnableMandalConfigBindings({
            @EnableMandalConfigBinding(prefix = "mandal.application", type = ApplicationConfig.class),
            @EnableMandalConfigBinding(prefix = "mandal.module", type = ModuleConfig.class),
            @EnableMandalConfigBinding(prefix = "mandal.registry", type = RegistryConfig.class),
            @EnableMandalConfigBinding(prefix = "mandal.protocol", type = ProtocolConfig.class),
            @EnableMandalConfigBinding(prefix = "mandal.monitor", type = MonitorConfig.class),
            @EnableMandalConfigBinding(prefix = "mandal.provider", type = ProviderConfig.class),
            @EnableMandalConfigBinding(prefix = "mandal.consumer", type = ConsumerConfig.class)
    })
    public static class Single{

    }

    @EnableMandalConfigBindings({
            @EnableMandalConfigBinding(prefix = "mandal.applications", type = ApplicationConfig.class, multiple = true),
            @EnableMandalConfigBinding(prefix = "mandal.modules", type = ModuleConfig.class, multiple = true),
            @EnableMandalConfigBinding(prefix = "mandal.registries", type = RegistryConfig.class, multiple = true),
            @EnableMandalConfigBinding(prefix = "mandal.protocols", type = ProtocolConfig.class, multiple = true),
            @EnableMandalConfigBinding(prefix = "mandal.monitors", type = MonitorConfig.class, multiple = true),
            @EnableMandalConfigBinding(prefix = "mandal.providers", type = ProviderConfig.class, multiple = true),
            @EnableMandalConfigBinding(prefix = "mandal.consumers", type = ConsumerConfig.class, multiple = true)
    })
    public static class Multiple{

    }
}
