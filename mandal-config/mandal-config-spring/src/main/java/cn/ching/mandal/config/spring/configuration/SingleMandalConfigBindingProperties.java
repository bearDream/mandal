package cn.ching.mandal.config.spring.configuration;

import cn.ching.mandal.config.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static cn.ching.mandal.config.spring.utils.MandalUtils.Mandal_PREFIX;

/**
 *
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: Single mandal config binding{@link ConfigurationProperties} with prefix "mandal."
 **/
@Data
@ConfigurationProperties(prefix = Mandal_PREFIX)
public class SingleMandalConfigBindingProperties {

    /**
     * {@link ApplicationConfig} property
     */
    @NestedConfigurationProperty
    private ApplicationConfig applicationConfig;

    /**
     * {@link ModuleConfig} property
     */
    @NestedConfigurationProperty
    private ModuleConfig moduleConfig;

    /**
     * {@link RegistryConfig} property
     */
    @NestedConfigurationProperty
    private RegistryConfig registryConfig;

    /**
     * {@link ProtocolConfig} property
     */
    @NestedConfigurationProperty
    private ProtocolConfig protocolConfig;

    /**
     * {@link MonitorConfig} property
     */
    @NestedConfigurationProperty
    private MonitorConfig monitorConfig;

    /**
     * {@link ProviderConfig} property
     */
    @NestedConfigurationProperty
    private ProviderConfig providerConfig;

    /**
     * {@link ConsumerConfig} property
     */
    @NestedConfigurationProperty
    private ConsumerConfig consumerConfig;
}
