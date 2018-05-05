package cn.ching.mandal.config.spring.context.event;

import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.config.spring.utils.MandalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.SortedMap;

/**
 * {@link ConfigUtils#getProperties()}
 *
 * last execution.
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: listener application and config it.
 **/
@Order(Integer.MAX_VALUE)
public class OverrideMandalConfigApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        final Logger logger = LoggerFactory.getLogger(getClass());

        ConfigurableEnvironment environment = event.getEnvironment();

        boolean override = environment.getProperty(MandalUtils.OVERRIDE_CONFIG_PROPERTY_NAME, boolean.class, MandalUtils.DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE);

        if (override){

            SortedMap<String, Object> mandalProperties = MandalUtils.filterDubboProperties(environment);

            ConfigUtils.getProperties().putAll(mandalProperties);
            if (logger.isInfoEnabled()){
                logger.info("Mandal config was overridden by externalized configuration. {}", mandalProperties);
            }
        }else {
            if (logger.isInfoEnabled()){
                logger.info("Disabled override Mandal config caused by property ={}={}", MandalUtils.OVERRIDE_CONFIG_PROPERTY_NAME, override);
            }
        }
    }
}
