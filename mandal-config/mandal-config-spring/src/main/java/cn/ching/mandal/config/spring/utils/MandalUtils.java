package cn.ching.mandal.config.spring.utils;

import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: system utils
 **/
public class MandalUtils {

    /**
     * line separator
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");


    /**
     * The separator of property name
     */
    public static final String PROPERTY_NAME_SEPARATOR = ".";

    /**
     * The prefix of property name of Mandal
     */
    public static final String Mandal_PREFIX = "mandal";

    /**
     * The prefix of property name for Mandal scan
     */
    public static final String Mandal_SCAN_PREFIX = Mandal_PREFIX + PROPERTY_NAME_SEPARATOR + "scan";

    /**
     * The prefix of property name for Mandal Config.ØØ
     */
    public static final String Mandal_CONFIG_PREFIX = Mandal_PREFIX + PROPERTY_NAME_SEPARATOR + "config";

    /**
     * The property name of base packages to scan
     * <p>
     * The default value is empty set.
     */
    public static final String BASE_PACKAGES_PROPERTY_NAME = Mandal_SCAN_PREFIX + PROPERTY_NAME_SEPARATOR + "basePackages";

    /**
     * The property name of multiple properties binding from externalized configuration
     * <p>
     * The default value is {@link #DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE}
     */
    public static final String MULTIPLE_CONFIG_PROPERTY_NAME = Mandal_CONFIG_PREFIX + PROPERTY_NAME_SEPARATOR + "multiple";

    /**
     * The default value of multiple properties binding from externalized configuration
     */
    public static final boolean DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE = false;

    /**
     * The property name of override Mandal config
     * <p>
     * The default value is {@link #DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE}
     */
    public static final String OVERRIDE_CONFIG_PROPERTY_NAME = Mandal_CONFIG_PREFIX + PROPERTY_NAME_SEPARATOR + "override";

    /**
     * The default property value of  override Mandal config
     */
    public static final boolean DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE = true;

    /**
     * The git URL of Mandal
     */
    public static final String MANDAL_GIT_URL = "https://github.com/bearDream/mandal.git";

    /**
     * Filters Dubbo Properties from {@link ConfigurableEnvironment}
     *
     * @param environment {@link ConfigurableEnvironment}
     * @return Read-only SortedMap
     */
    public static SortedMap<String, Object> filterDubboProperties(ConfigurableEnvironment environment) {

        SortedMap<String, Object> mandalProperties = new TreeMap<>();

        Map<String, Object> properties = EnvironmentUtils.extractProperties(environment);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();

            if (propertyName.startsWith(Mandal_PREFIX + PROPERTY_NAME_SEPARATOR)) {
                mandalProperties.put(propertyName, entry.getValue());
            }

        }

        return Collections.unmodifiableSortedMap(mandalProperties);

    }
}
