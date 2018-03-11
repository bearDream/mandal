package cn.ching.mandal.common.utils;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2018/1/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

    private static volatile Properties PROPERTIES;

    private static Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\s*\\{?\\s*([\\._0-9a-zA-Z]+)\\s*\\}?");

    private static int PID = -1;

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

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (!StringUtils.isEmpty(value)){
            return value;
        }
        Properties properties = getProperties();
        return replaceProperties(properties.getProperty(key, defaultValue), (Map) properties);
    }

    private static Properties getProperties() {
        if (Objects.isNull(PROPERTIES)){
            synchronized (ConfigUtils.class){
                if (Objects.isNull(PROPERTIES)){
                    String path = System.getProperty(Constants.MANDAL_PROPERTIES_KEY);
                    if (StringUtils.isEmpty(path)){
                        path = System.getenv(Constants.MANDAL_PROPERTIES_KEY);
                        if (StringUtils.isEmpty(path)){
                            path = Constants.DEFAULT_MANDAL_PROPERTIES;
                        }
                    }
                    PROPERTIES = ConfigUtils.loadProperties(path, false, true);
                }
            }
        }
        return PROPERTIES;
    }

    private static String replaceProperties(String expression, Map<String, String> params) {
        if (StringUtils.isEmpty(expression) || expression.indexOf("$") < 0){
            return expression;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()){
            String key = matcher.group(1);
            String value = System.getProperty(key);
            if (Objects.isNull(value) && !Objects.isNull(params)){
                value = params.get(key);
            }
            if (Objects.isNull(value)){
                value = "";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Load properties file to {@link Properties} from class path.
     * @param fileName
     * @param allowMultiFile
     * @param optional
     * @return loaded {@link Properties} content.
     * <li>return empty Properties if not found prop file</li>
     * <li>return merged multi file if found multi prop files.</li>
     */
    private static Properties loadProperties(String fileName, boolean allowMultiFile, boolean optional) {
        Properties properties = new Properties();
        if (fileName.startsWith("/")){
            try {
                FileInputStream in = new FileInputStream(fileName);
                try {
                    properties.load(in);
                }finally {
                    in.close();
                }
            }catch (Throwable t){
                logger.warn("failed load properties" + fileName + " file from " + fileName + "(ignore this file)" + t.getMessage(), t);
            }
            return properties;
        }

        List<java.net.URL> list = new ArrayList<>();
        try {
            Enumeration<java.net.URL> urls = ClassHelper.getClassLoader().getResources(fileName);
            list = new ArrayList<>();
            while (urls.hasMoreElements()){
                list.add(urls.nextElement());
            }
        }catch (Throwable t){
            logger.warn("failed load file " + fileName + t.getMessage(), t);
        }

        if (list.size()  == 0){
            if (!optional){
                logger.warn("No " + fileName + "found on classpath.");
            }
            return properties;
        }

        // if not allow merge prop, then log it and load filename.
        if (!allowMultiFile){
            if (list.size() > 1){
                String errMsg = String.format("only 1 %s file is expected, but %d mandal.properties files found on class path: %s", fileName, list.size(), list.toString());
                logger.warn(errMsg);
            }

            try {
                properties.load(ClassHelper.getClassLoader().getResourceAsStream(fileName));
            }catch (Throwable t){
                logger.warn("failed to load " + fileName + " file from " + fileName + "(ignore this file)" + t.getMessage(), t);
            }
            return properties;
        }

        // merge multi prop file.
        logger.info("load " + fileName + " properties file from " + list);
        for (URL url : list){
            try {
                Properties p = new Properties();
                InputStream in = url.openStream();
                if (!Objects.isNull(in)){
                    try {
                        p.load(in);
                        properties.putAll(p);
                    }finally {
                        try {
                            in.close();
                        }catch (Throwable t){

                        }
                    }
                }
            } catch (IOException e) {
                String msg = String.format("faile to load %s file from %s (ignore this file).", fileName, url);
                logger.warn(msg);
            }
        }

        return properties;
    }

    public static int getPID() {
        if (PID < 0){
            try {
                RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
                String name = runtime.getName();
                PID = Integer.parseInt(name.substring(0, name.indexOf('@')));
            }catch (Throwable t){
                PID = 0;
            }
        }
        return PID;
    }
}
