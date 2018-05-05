package cn.ching.mandal.config.spring.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static cn.ching.mandal.config.spring.utils.MandalUtils.DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE;
import static cn.ching.mandal.config.spring.utils.MandalUtils.DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE;
import static cn.ching.mandal.config.spring.utils.MandalUtils.Mandal_CONFIG_PREFIX;

/**
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: config properties with prefix "mandal.config"
 **/
@Data
@ConfigurationProperties(prefix = Mandal_CONFIG_PREFIX)
public class MandalConfigProperties {

    /**
     * default is single config.
     */
    private boolean multiple = DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE;

    /**
     * default override config.
     */
    private boolean override = DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE;

}
