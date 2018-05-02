package cn.ching.mandal.config.spring.utils;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 2018/4/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class PropertySourcesUtils {

    public static Map<String, String> getSubProperties(PropertySources propertySources, String prefix){

        Map<String, String> subProperties = new LinkedHashMap<>();

        String normalPrefix = prefix.endsWith(".") ? prefix : prefix + ".";

        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof EnumerablePropertySource){
                for (String name : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
                    if (name.startsWith(normalPrefix)){
                        String subName = name.substring(normalPrefix.length());
                        Object value = propertySource.getProperty(name);
                        subProperties.put(subName, String.valueOf(value));
                    }
                }
            }
        }

        return subProperties;
    }
}
