package cn.ching.mandal.common.utils;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;

/**
 * 2018/1/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);


    private ConfigUtils(){}

    public static boolean isNotEmpty(String val){
        return !isEmpty(val);
    }

    public static boolean isEmpty(String value){
        return value == null || value.length() == 0
                || "false".equalsIgnoreCase(value)
                || "0".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value)
                || "N/A".equalsIgnoreCase(value);
    }

    public static boolean isDefault(String val) {
        return "true".equalsIgnoreCase(val) || "default".equalsIgnoreCase(val);
    }
}
