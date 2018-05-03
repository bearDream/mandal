package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.ReflectUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.support.Parameter;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractConfig implements Serializable{

    protected final static Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    private static final long serialVersionUID = 5274263242004599258L;

    private final static int MAX_LENGTH = 200;
    private final static int MAX_PATH_LENGTH = 200;

    private final static Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");

    private final static Pattern PATTERN_MULTI_NAME = Pattern.compile("[,\\-._0-9a-zA-Z]+");

    private final static Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*]");

    private final static Pattern PATTERN_PATH = Pattern.compile("[/\\-$._09a-zA-Z]");

    private final static Pattern PATTERN_NAME_HAS_SYMBOL = Pattern.compile("[:*,/\\-._0-9a-zA-Z]+");

    private final static Pattern PATTERN_KEY = Pattern.compile("[*,\\-._0-9a-zA-Z]+");

    private final static Map<String, String> legacyProperties = new HashMap<>();

    private final static String[] SUFFIXES = new String[]{"Config", "Bean"};

    protected String id;

    static {
        legacyProperties.put("mandal.protocol.name", "mandal.service.protocol");
        legacyProperties.put("mandal.protocol.host", "mandal.service.server.host");
        legacyProperties.put("mandal.protocol.port", "mandal.service.server.port");
        legacyProperties.put("mandal.protocol.threads", "mandal.service.max.thread.pool.size");
        legacyProperties.put("mandal.consumer.timeout", "mandal.service.invoke.timeout");
        legacyProperties.put("mandal.consumer.retries", "mandal.service.max.retries.providers");
        legacyProperties.put("mandal.consumer.check", "mandal.service.allow.no.provider");
        legacyProperties.put("mandal.service.url", "mandal.service.address");
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (logger.isInfoEnabled()){
                logger.info("Run shutdown hook.");
            }
            ProtocolConfig.destroyAll();
        }, "MandalShutdownHook"));
    }

    @Parameter(exclude = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    protected static void appendProperties(AbstractConfig config){
        if (Objects.isNull(config)){
            return;
        }
        String prefix = "mandal." + getTagName(config.getClass()) + ".";
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if (name.length() > 3 && name.startsWith("set")
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 1
                        && isPrimitive(method.getParameterTypes()[0])){
                    // result: getId -> id
                    String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), ".");

                    String val = null;
                    if (!StringUtils.isEmpty(config.getId())){
                        String p = prefix + config.getId() + "." + property;
                        val = System.getProperty(p);
                        if (!StringUtils.isEmpty(val)){
                            logger.info("Use System property " + p + " to config mandal.");
                        }
                    }
                    if (StringUtils.isEmpty(val)){
                        String p = prefix + property;
                        val = System.getProperty(p);
                        if (!StringUtils.isEmpty(val)){
                            logger.info("Use System property " + p + " to config mandal.");
                        }
                    }
                    if (StringUtils.isEmpty(val)){
                        Method getter;
                        try {
                            getter = config.getClass().getMethod("get" + name.substring(3), new Class<?>[0]);
                        }catch (NoSuchMethodException e){
                            try {
                                getter = config.getClass().getMethod("is" + name.substring(3), new Class<?>[0]);
                            }catch (NoSuchMethodException e1){
                                getter = null;
                            }
                        }
                        if (!Objects.isNull(getter)){
                            if (getter.invoke(config, new Object[0]) == null){
                                if (!StringUtils.isEmpty(config.getId())){
                                    val = ConfigUtils.getProperty(prefix + config.getId() + "." + property);
                                }
                                if (StringUtils.isEmpty(val)){
                                    val = ConfigUtils.getProperty(prefix + property);
                                }
                                if (StringUtils.isEmpty(val)){
                                    String legacyKey = legacyProperties.get(prefix + property);
                                    if (!Objects.isNull(legacyKey) && legacyKey.length() > 0){
                                        val = convertLegacyValue(legacyKey, ConfigUtils.getProperty(legacyKey));
                                    }
                                }
                            }
                        }
                    }
                    if (!StringUtils.isEmpty(val)){
                        method.invoke(config, new Object[]{convertPrimitive(method.getParameterTypes()[0], val)});
                    }
                }
            }catch (Exception e){
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected static void appendParameters(Map<String, String> parameters, Object config){
        appendParameters(parameters, config, null);
    }

    protected static void appendParameters(Map<String, String> parameters, Object config, String prefix) {
        if (Objects.isNull(config)){
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                // only get method can append parameters.
                if ((name.startsWith("get") || name.startsWith("is"))
                        && !"getClass".equals(name)
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 0
                        && isPrimitive(method.getReturnType())){
                    // like:  public Map getName()
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (Object.class == method.getReturnType() || !Objects.isNull(parameter) && parameter.exclude()){
                        continue;
                    }
                    int i = name.startsWith("get") ? 3 : 2;
                    String prop = StringUtils.camelToSplitName(name.substring(i, i+1).toLowerCase() + name.substring(i + 1), ",");
                    String key;
                    if (!Objects.isNull(parameter) && !Objects.isNull(parameter.key()) && parameter.key().length() > 0){
                        key = parameter.key();
                    }else {
                        key = prop;
                    }
                    Object value = method.invoke(config, new Object[0]);
                    String str = String.valueOf(value).trim();
                    if (!Objects.isNull(value) && str.length() > 0){
                        if (!Objects.isNull(parameter) && parameter.escaped()){
                            str = URL.encode(str);
                        }
                        if (!Objects.isNull(parameter) && parameter.append()){
                            String pre = parameters.get(Constants.DEFAULT_KEY + "." + key);
                            if (!StringUtils.isEmpty(pre)){
                                str = pre + "," + str;
                            }
                            pre = parameters.get(key);
                            if (!StringUtils.isEmpty(pre)){
                                str = pre + "," + str;
                            }
                        }
                        if (!StringUtils.isEmpty(prefix)){
                            key = prefix + "." + key;
                        }
                        parameters.put(key, str);
                    }else if (!Objects.isNull(parameter) && parameter.required()){
                        // if this get method has @Parameter annotation and required is true, but value is null, then throw exception.
                        throw new IllegalStateException(config.getClass().getSimpleName() + "." + key + " == null");
                    }
                }else if ("getParameters".equals(name)
                            && Modifier.isPublic(method.getModifiers())
                            && method.getParameterTypes().length == 0
                            && method.getReturnType() == Map.class){
                    // like: public Map getParameters()
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    if (!CollectionUtils.isEmpty(map)){
                        String pre = StringUtils.isEmpty(prefix) ? "" : prefix + ".";
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            parameters.put(pre + entry.getKey().replace('-', '.'), entry.getValue());
                        }
                    }
                }
            }catch (Exception e){
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    protected static void appendAttributes(Map<Object, Object> parameters, Object config) {
        appendAttributes(parameters, config, null);
    }

    protected static void appendAttributes(Map<Object, Object> parameters, Object config, String prefix) {
        if (Objects.isNull(config)){
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if ((name.startsWith("get") || name.startsWith("is"))
                        && !"getClass".equals(name)
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length==0
                        && isPrimitive(method.getReturnType())){
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (Objects.isNull(parameter) || !parameter.attribute()){
                        continue;
                    }
                    String key;
                    if (!Objects.isNull(parameter) && !Objects.isNull(parameter.key()) && parameter.key().length() > 0){
                        key = parameter.key();
                    }else {
                        int i = name.startsWith("get") ? 3 : 2;
                        key = name.substring(i, i+1).toLowerCase() + name.substring(i+1);
                    }
                    Object value = method.invoke(config, new Object[0]);
                    if (!Objects.isNull(value)){
                        if (prefix != null && prefix.length() > 0){
                            key = prefix + "." + key;
                        }
                        parameters.put(key, value);
                    }
                }
            }catch (Exception e){
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    protected void appendAnnotation(Class<?> annotationClass, Object annotation){
        Method[] methods = annotationClass.getMethods();
        for (Method method : methods) {
            if (method.getDeclaringClass() != Object.class
                    && method.getReturnType() != void.class
                    && method.getParameterTypes().length == 0
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())){
                try {
                    String property = method.getName();
                    if ("interfaceClass".equals(property) || "interfaceName".equals(property)){
                        property = "interface";
                    }
                    // build up camel name.
                    String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
                    Object value = method.invoke(annotation, new Object[0]);
                    if (!Objects.isNull(value) && !value.equals(method.getDefaultValue())){
                        Class<?> parameterType = ReflectUtils.getBoxedClass(method.getReturnType());
                        if ("filter".equals(property) || "listener".equals(property)){
                            parameterType = String.class;
                            value = StringUtils.join((String[]) value, ",");
                        }else if ("parameters".equals(property)){
                            parameterType = Map.class;
                            value = CollectionUtils.toStringMap((String[]) value);
                        }
                        try {
                            Method setterMethod = getClass().getMethod(setter, new Class<?>[]{parameterType});
                            setterMethod.invoke(this, new Object[]{value});
                        }catch (NoSuchMethodException e){
                        }
                    }
                }catch (Throwable t){
                    logger.error(t.getMessage(), t);
                }
            }
        }
    }

    protected static void checkExtension(Class<?> type, String property, String value){
        checkName(property, value);
        if (!StringUtils.isEmpty(value)
                && !ExtensionLoader.getExtensionLoader(type).hasExtension(value)){
            throw new IllegalStateException("No such extension " + value + " for " + property + "/" + type.getName());
        }
    }

    protected static void checkMultiExtension(Class<?> type, String property, String value){
        checkMultiName(property, value);
        if (!StringUtils.isEmpty(value)){
            // 123,abc,567 -> 123 abc 567
            String[] values = value.split("\\s*[,]+\\s*");
            for (String s : values) {
                if (s.startsWith(Constants.REMOVE_VALUE_PREFIX)){
                    s = s.substring(1);
                }
                if (Constants.DEFAULT_KEY.equals(s)){
                    continue;
                }
                if (!ExtensionLoader.getExtensionLoader(type).hasExtension(value)){
                    throw new IllegalStateException("No such extension " + s + " for " + property + "/" + type.getName());
                }
            }
        }

    }

    protected static void checkProperty(String property, String value, int max, Pattern pattern){
        if (Objects.isNull(value)){
            return;
        }
        if (value.length() > max){
            throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" is longer than " + max);
        }
        if (!Objects.isNull(pattern)){
            Matcher matcher = pattern.matcher(value);
            if(!matcher.matches()){
                throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" contains illegal character, only digit, letter, '-', '_' and '.' is legal.");
            }
        }
    }

    protected static void checkParameterName(Map<String, String> parameters){
        if (CollectionUtils.isEmpty(parameters)){
            return;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            checkNameHasSymbol(entry.getKey(), entry.getValue());
        }
    }

    protected static void checkLength(String property, String value){
        checkProperty(property, value, MAX_LENGTH, null);
    }

    protected static void checkPathLength(String property, String value){
        checkProperty(property, value, MAX_LENGTH, null);
    }

    protected static void checkName(String property, String value){
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }

    protected static void checkNameHasSymbol(String property, String value){
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME_HAS_SYMBOL);
    }

    protected static void checkKey(String property, String value){
        checkProperty(property, value, MAX_LENGTH, PATTERN_KEY);
    }

    protected static void checkMultiName(String property, String value){
        checkProperty(property, value, MAX_LENGTH, PATTERN_MULTI_NAME);
    }

    protected static void checkPathName(String property, String value){
        checkProperty(property, value, MAX_LENGTH, PATTERN_PATH);
    }

    protected static void checkMethodName(String property, String value){
        checkProperty(property, value, MAX_LENGTH, PATTERN_METHOD_NAME);
    }

    private static String convertLegacyValue(String key, String value) {
        if (!StringUtils.isEmpty(key)){
            if ("mandal.service.max.retry.providers".equals(key)){
                return String.valueOf(Integer.parseInt(value) - 1);
            }else if ("mandal.service.allow.no.provider".equals(key)){
                return String.valueOf(!Boolean.parseBoolean(value));
            }
        }
        return value;
    }

    private static Object convertPrimitive(Class<?> type, String value) {
        if (type == char.class || type == Character.class){
            return value.length() > 0 ? value.charAt(0) : '\0';
        }else if (type == boolean.class || type == Boolean.class){
            return Boolean.valueOf(value);
        }else if (type == byte.class || type == Byte.class){
            return Byte.valueOf(value);
        }else if (type == short.class || type == Short.class){
            return Short.valueOf(value);
        }else if (type == int.class || type == Integer.class){
            return Integer.valueOf(value);
        }else if (type == long.class || type == Long.class) {
            return Long.valueOf(value);
        } else if (type == float.class || type == Float.class) {
            return Float.valueOf(value);
        } else if (type == double.class || type == Double.class) {
            return Double.valueOf(value);
        }
        return value;
    }

    /**
     * xxxBean or yyyConfig  -->  xxx or yyy
     * @param cls
     * @return tag
     */
    private static String getTagName(Class<?> cls){
        String tag = cls.getSimpleName();
        for (String suffix : SUFFIXES) {
            if (tag.endsWith(suffix)){
                tag = tag.substring(0, tag.length() - suffix.length());
                break;
            }
        }
        tag = tag.toLowerCase();
        return tag;
    }

    private static boolean isPrimitive(Class<?> type){
        return type.isPrimitive()
                || type == String.class
                || type == Character.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Object.class;
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<mandal:");
            sb.append(getTagName(getClass()));
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                try {
                    String name = method.getName();
                    if ((name.startsWith("get") || name.startsWith("is"))
                            && !"getClass".equals(name) && !"get".equals(name) && !"is".equals(name)
                            && Modifier.isPublic(method.getModifiers())
                            && method.getParameterTypes().length == 0
                            && isPrimitive(method.getReturnType())){
                        int i = name.startsWith("get") ? 3 : 2;
                        String key = name.substring(i, i+1).toLowerCase() + name.substring(i+1);
                        Object val = method.invoke(this, new Object[0]);
                        if (!Objects.isNull(val)){
                            sb.append(" ");
                            sb.append(key);
                            sb.append("=\"");
                            sb.append(val);
                            sb.append("\"");
                        }
                    }
                }catch (Exception e){
                    logger.warn(e.getMessage(), e);
                }
            }
            sb.append(" />");
            return sb.toString();
        }catch (Throwable t){
            logger.warn(t.getMessage(), t);
            return super.toString();
        }
    }
}
