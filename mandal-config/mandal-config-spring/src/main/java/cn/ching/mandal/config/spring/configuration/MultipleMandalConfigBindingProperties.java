package cn.ching.mandal.config.spring.configuration;

import cn.ching.mandal.config.*;
import cn.ching.mandal.config.spring.context.annotation.EnableMandalConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.ching.mandal.config.spring.utils.MandalUtils.Mandal_PREFIX;

/**
 *
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: Multiple mandal config binding{@link ConfigurationProperties} with prefix "mandal."
 * @see EnableMandalConfig#multiple()
 **/
@Data
@ConfigurationProperties(prefix = Mandal_PREFIX)
public class MultipleMandalConfigBindingProperties {

    /**
     * {@link ApplicationConfig} property
     */
    private Map<String, ApplicationConfig> applications = new LinkedHashMap<>();

    /**
     * {@link ModuleConfig} property
     */
    @NestedConfigurationProperty
    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    /**
     * {@link RegistryConfig} property
     */
    @NestedConfigurationProperty
    private Map<String, RegistryConfig> registries = new LinkedHashMap<>();

    /**
     * {@link ProtocolConfig} property
     */
    @NestedConfigurationProperty
    private Map<String, ProtocolConfig> protocols = new LinkedHashMap<>();

    /**
     * {@link MonitorConfig} property
     */
    @NestedConfigurationProperty
    private Map<String, MonitorConfig> monitorConfig = new LinkedHashMap<>();

    /**
     * {@link ProviderConfig} property
     */
    @NestedConfigurationProperty
    private Map<String, ProviderConfig> providerConfig = new LinkedHashMap<>();

    /**
     * {@link ConsumerConfig} property
     */
    @NestedConfigurationProperty
    private Map<String, ConsumerConfig> consumerConfig = new LinkedHashMap<>();
}
