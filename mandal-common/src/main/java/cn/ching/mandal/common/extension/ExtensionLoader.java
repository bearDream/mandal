package cn.ching.mandal.common.extension;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.compiler.Compiler;
import cn.ching.mandal.common.extension.support.ActivateComparator;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ClassHolder;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 2018/1/5
 * load extension
 * wrapper class must have constructor as Protocol as parameter.
 * @since 1.8
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICE_DIR = "META-INF/services/";

    private static final String MANDAL_DIR = "META-INF/mandal/";

    private static final String MANDAL_INTERNAL_DIR = MANDAL_DIR + "internal/";

    /**
     * split {@link SPI#value()} to string[]
     * @see SPI
     */
    private static final Pattern NAME_SEPERATOR = Pattern.compile("\\s*[,]+\\s*");

    private static final ConcurrentHashMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    private static final ConcurrentHashMap<Class<?>, Object> EXTENSION_INSTANCE = new ConcurrentHashMap<Class<?>, Object>();

    private Class<?> type;

    // when invoke constructor objectFactory has been instancing
    private final ExtensionFactory objectFactory;

    // implication class name
    private final ConcurrentHashMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();

    private final ClassHolder<Map<String, Class<?>>> cachedClasses = new ClassHolder<Map<String, Class<?>>>();

    private final Map<String, Activate> cachedActivates = new ConcurrentHashMap<String, Activate>();

    private final ConcurrentHashMap<String, ClassHolder<Object>> cachedInstances = new ConcurrentHashMap<String, ClassHolder<Object>>();

    private final ClassHolder<Object> cachedAdaptiveInstance = new ClassHolder<Object>();

    private volatile Class<?> cachedAdaptiveClass = null;

    private String cachedDefaultName;

    private volatile Throwable createAdaptiveInstanceError;

    private Set<Class<?>> cachedWrapperClasses;

    private Map<String, IllegalStateException> exception = new ConcurrentHashMap<String, IllegalStateException>();

    /**
     * getExtensionLoader -> getAdaptiveExtension -> createAdaptiveInstance -> getAdaptiveExtensionClass -> getExtensionClasses -> loadExtensionClasses -> loadFile ->
     * createAdaptiveExtensionClass ->
     * @param type
     */
    private ExtensionLoader(Class<?> type){
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }

    /**
     * get extension，
     * if it has been created, then get it from {@link ExtensionLoader#EXTENSION_LOADERS}
     * if not create, then init {@link ExtensionLoader#ExtensionLoader(Class)}
     * @param type
     * @param <T>
     * @return
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (Objects.isNull(type)){
            throw new IllegalStateException("failed getExtensionLoader, cause by type is null");
        }
        if (!type.isInterface()){
            throw new IllegalStateException("type :" + type + "is not a interface");
        }
        if (!withExtensionAnnotation(type)){
            throw new IllegalStateException("type :" + type + "haven't "+SPI.class.getSimpleName()+" annotation, so type is not a extensionLoader class");
        }
        ExtensionLoader<T> extensionLoader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (Objects.isNull(extensionLoader)){
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            extensionLoader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    public String getExtensionName(T extesionInstances){
        return getExtensionName(extesionInstances.getClass());
    }

    public String getExtensionName(Class<?> extensionClass){
        return cachedNames.get(extensionClass);
    }


    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }

    /**
     * get activate extension list
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @return extension list which activate
     */
    public List<T> getActivateExtension(URL url, String key){
        return getActivateExtension(url, key, null);
    }

    /**
     * get activate extension list
     * @param url url
     * @param values extension point names
     * @return extension list which activate
     */
    public List<T> getActivateExtension(URL url, String[] values){
        return getActivateExtension(url, values, null);
    }

    /**
     * get activate extension list
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @param group group
     * @return
     */
    public List<T> getActivateExtension(URL url, String key, String group){
        String value = url.getParameter(key);
        return getActivateExtension(url, StringUtils.isBlank(value) ? null : Constants.COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * get activate extension list
     * @param url url
     * @param value extension point names
     * @param group group
     * @return
     * @see {@link Activate}
     */
    public List<T> getActivateExtension(URL url, String[] value, String group){
        List<T> exts = new ArrayList<>();
        List<String> names = Objects.isNull(value) ? new ArrayList<>() : Arrays.asList(value);
        // find name contain "-mandal". load all mandal activate extension
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)){
            getExtensionClasses();
            for (Map.Entry<String, Activate> entry : cachedActivates.entrySet()){
                Activate activate = entry.getValue();
                String name = entry.getKey();
                if (isMatchGroup(group, activate.group())){
                    T ext = getExtension(name);
                    if (!names.contains(name)
                            && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)
                            && isActive(activate, url)){
                        exts.add(ext);
                    }
                }
            }
            // sort extension by order/before/after
            Collections.sort(exts, ActivateComparator.COMPARATOR);
        }
        List<T> nameExtensions = new ArrayList<>();
        names.stream().filter(name -> !name.startsWith(Constants.REMOVE_VALUE_PREFIX) && !names.contains(Constants.REMOVE_VALUE_PREFIX + name))
                .forEach(name -> {
                    if (Constants.DEFAULT_KEY.equals(name)){
                        if (nameExtensions.size() > 0){
                            exts.addAll(nameExtensions);
                            nameExtensions.clear(); // avoid duplicate add to exts
                        }
                    }else {
                        T ext = getExtension(name);
                        nameExtensions.add(ext);
                    }
                });
        if (nameExtensions.size() > 0){
            exts.addAll(nameExtensions);
        }
        return exts;
    }

    /**
     * mathch group exists in groups[]
     * @param group
     * @param groups
     * @return
     */
    private boolean isMatchGroup(String group, String[] groups){
        if (Objects.isNull(groups) || groups.length == 0){
            return true;
        }
        if (Objects.isNull(groups) && groups.length > 0){
            for (String g : groups){
                if (g.equals(group)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * find url parameters in activate values
     * @param activate
     * @param url
     * @return
     */
    private boolean isActive(Activate activate, URL url){
        String[] keys = activate.value();
        if (Objects.isNull(keys) || keys.length == 0){
            return true;
        }

        if (Objects.isNull(keys) && keys.length > 0){
            for (String key : keys){
                for (Map.Entry<String, String> entry : url.getParameters().entrySet()){
                    String k = entry.getKey();
                    String v = entry.getValue();
                    if ((k.equals(key) || k.endsWith("." + key) && ConfigUtils.isNotEmpty(v))){
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * get adaptive instance
     * @return
     */
    public T getAdaptiveExtension() {
        Object instance = cachedAdaptiveInstance.get();
        if (Objects.isNull(instance)){
            if (Objects.isNull(createAdaptiveInstanceError)){
                synchronized (cachedAdaptiveInstance) {
                    try {
                        instance = createAdaptiveInstance();
                        cachedAdaptiveInstance.set(instance);
                    } catch (Throwable t){
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("faile to create AdaptiveInstance:" + t.toString(), t);
                    }
                }
            }
        }
        return (T) instance;
    }

    private T createAdaptiveInstance() {
        try {
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        }catch (Exception e){
            throw new IllegalStateException("can't create adaptive extension:" + type + " cause: " + e.getMessage(), e);
        }
    }

    /**
     * inject instance property
     * @param instance
     * @return
     */
    private T injectExtension(T instance) {
        try {
            if (!Objects.isNull(objectFactory)){
                Method[] methods = instance.getClass().getMethods();
                for (Method method : methods){
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length ==1
                            && Modifier.isPublic(method.getModifiers())){
                        // get property type
                        Class<?> pt = method.getParameterTypes()[0];
                        try {
                            // get property name
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            // get Protocol$Adaptive
                            Object object = objectFactory.getExtension(pt, property);
                            if (!Objects.isNull(object)){
                                // inject set method Protocol$Adaptive
                                method.invoke(instance, object);
                            }
                        } catch (Exception e){
                            logger.error("failed inject property method: " + method.getName() + "of instance: " + type.getName() + ":" + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        return instance;
    }

    private Map<String, Class<?>> getExtensionClasses(){
        Map<String, Class<?>> classes = cachedClasses.get();
        if (Objects.isNull(classes)){
            synchronized (cachedClasses){
                classes = cachedClasses.get();
                if (Objects.isNull(classes)){
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    // synchronized load extension
    private Map<String,Class<?>> loadExtensionClasses() {
        final SPI annotation = type.getAnnotation(SPI.class);
        if (Objects.nonNull(annotation)){
            String val = annotation.value();
            if (StringUtils.notBlank(val)){
                String[] vals = NAME_SEPERATOR.split(val);
                if (vals.length > 1){
                    throw new IllegalStateException("more than 1 extension name: " + type.getName() + " " + Arrays.toString(vals));
                }
                if (vals.length==1){
                    cachedDefaultName = vals[0];
                }
            }
        }

        // load extension
        Map<String, Class<?>> extensionClasses = new HashMap<>();
        loadFile(extensionClasses, MANDAL_INTERNAL_DIR);
        loadFile(extensionClasses, MANDAL_DIR);
        loadFile(extensionClasses, SERVICE_DIR);
        return extensionClasses;
    }

    /**
     *
     * load extension by file
     * <p>
     *     file like this:  xxxx=xxxx
     * </p>
     * @param extensionClasses
     * @param dir
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        // filename is classname
        String fileName = dir + type.getName();
        try {
            Enumeration<java.net.URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (Objects.isNull(classLoader)){
                urls = ClassLoader.getSystemResources(fileName);
            }else {
                urls = classLoader.getResources(fileName);
            }
            if (Objects.nonNull(urls)){
                while (urls.hasMoreElements()){
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null){
                                line = line.trim();
                                if (line.length() > 0){
                                    try {
                                        String name = null;
                                        int equalIndex = line.indexOf("=");
                                        if (equalIndex > 0){
                                            name = line.substring(0, equalIndex).trim(); // spi("name")
                                            line = line.substring(equalIndex+1).trim(); //implication class name
                                        }
                                        if (line.length() > 0){
                                            Class clazz = Class.forName(line, true, classLoader);
                                            // judge clazz is type implication class
                                            if (!type.isAssignableFrom(clazz)){
                                                throw new IllegalArgumentException("failed when load extension class ( " + clazz.getSimpleName() +
                                                        " ),is not class (" + clazz + "subtype");
                                            }
                                            // judge clazz has @Adaptive
                                            if (clazz.isAnnotationPresent(Adaptive.class)){
                                                if (Objects.isNull(cachedAdaptiveClass)){
                                                    cachedAdaptiveClass = clazz;
                                                }else {
                                                    throw new IllegalStateException("More than 1 adaptive class found: "
                                                            + cachedAdaptiveClass.getClass().getName()
                                                            + ", "
                                                            + clazz.getClass().getName());
                                                }
                                            }else {
                                                try {
                                                    // judge clazz has type as parameter construct method! if not have constructor then throw NoSuchMethodException
                                                    // get wrapper class.(only for wrapper Design pattern)
                                                    clazz.getConstructor(type);
                                                    Set<Class<?>> wrappers = cachedWrapperClasses;
                                                    if (Objects.isNull(wrappers)){
                                                        cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                                                        wrappers = cachedWrapperClasses;
                                                    }
                                                    wrappers.add(clazz);
                                                }catch (NoSuchMethodException e){
                                                    clazz.getConstructors();
                                                    if (StringUtils.isBlank(name)){
                                                        if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                                                && clazz.getSimpleName().endsWith(type.getSimpleName())){
                                                            name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                        }else {
                                                            throw new IllegalStateException("no such extension name : " + clazz.getName() + "in config file" + url);
                                                        }
                                                    }
                                                    String[] names = NAME_SEPERATOR.split(name);
                                                    if (names!=null && names.length>0){
                                                        Activate activate = (Activate) clazz.getAnnotation(Activate.class);
                                                        // judge clazz has Activate annotation, if has it put to cachedActivates
                                                        if (Objects.nonNull(activate)){
                                                            cachedActivates.putIfAbsent(names[0], activate);
                                                        }
                                                        // one interface support more implication.
                                                        for (String n : names){
                                                            if (!cachedNames.containsKey(n)){
                                                                cachedNames.put(clazz, n);
                                                            }
                                                            Class<?> c = extensionClasses.get(clazz);
                                                            if (Objects.isNull(c)){
                                                                extensionClasses.putIfAbsent(n, clazz);
                                                            }else if (c != clazz){
                                                                throw new IllegalStateException("Duplicate extension " + type.getName() + "class: " + c.getName() + "on " + clazz.getName());
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    } catch (Throwable t){
                                        IllegalStateException e = new IllegalStateException("failed to load extension on loadFile interface( " + type.getName() + " ) line is ( " + line + ") url: " + url + " cause:{" + t.getMessage() + "}");
                                        exception.put(line, e);
                                    }
                                }
                            }
                        }finally {
                            reader.close();
                        }
                    } catch (Exception e){
                        logger.error("Exception when load extension (interface" + type.getName() + "class file: " + url + "cause: " + e);
                    }
                }
            }
        }catch (Exception e){
            logger.error("load extension interface: " + type.getName() + "description file : " + fileName + "cause t:" + e);
        }
    }

    private static ClassLoader findClassLoader(){
        return ExtensionLoader.class.getClassLoader();
    }

    public static ClassLoader findclassLoader(){
        return ExtensionLoader.class.getClassLoader();
    }

    /**
     * @see ExtensionLoader#createAdaptiveInstance
     * @return
     */
    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (Objects.nonNull(cachedAdaptiveClass)){
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * get cached extension classes
     * @return
     */
    public Set<String> getSupportedExtensions(){
        Map<String, Class<?>> clazz = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<>(clazz.keySet()));
    }

    // find extension impl by name
    public T getExtension(String name) {
        if (StringUtils.isBlank(name)){
            throw new IllegalArgumentException("Extension adapter name is null");
        }
        if (name.equals("true")){
            return getDefaultExtension();
        }
        ClassHolder holder = cachedInstances.get(name);
        if (Objects.isNull(holder)){
            cachedInstances.putIfAbsent(name, new ClassHolder<>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (Objects.isNull(instance)){
            synchronized (holder){
                instance = holder.get();
                if (Objects.isNull(instance)){
                    instance = creatExtension(name);
                    holder.set(instance);
                }
            }
        }

        return (T) instance;
    }

    /**
     * create extension instance
     * @param name
     * @return
     */
    private T creatExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (Objects.isNull(clazz)){
            throw findException(name);
        }

        try {
            T instance = (T) EXTENSION_INSTANCE.get(name);
            if (Objects.isNull(instance)){
                EXTENSION_INSTANCE.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCE.get(clazz);
            }

            injectExtension(instance);
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && wrapperClasses.size() > 0){
                for (Class<?> wrapperClass : wrapperClasses){
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance name: " + name + " class: " + type + " couldn't be instantiated " + t.getMessage(), t);
        }

    }

    /**
     * according name find exception. if can't find it, then return {@link ExtensionLoader#exception}
     * @param name
     * @return
     */
    private IllegalStateException findException(String name) {
        // find exception by name according to exception
        for (Map.Entry<String, IllegalStateException> entry : exception.entrySet()){
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())){
                return entry.getValue();
            }
        }
        // if can't find exception, then get all exception ;
        StringBuilder e = new StringBuilder("No such extension " + type.getName() + " by name :" + name);
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exception.entrySet()){
            if (i == 1){
                e.append(" possible cause by: ");
            }

            e.append("\r\n(");
            e.append(i++);
            e.append(entry.getKey());
            e.append("\r\n");
            e.append(entry.getValue());
        }
        return new IllegalStateException(e.toString());
    }

    public T getDefaultExtension() {
        getExtensionClasses();
        if (StringUtils.isBlank(cachedDefaultName) || cachedDefaultName.equals("true")){
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    /**
     * if no interface implication class, then generate implication class and compiler it.
     * @return
     */
    private Class<?> createAdaptiveExtensionClass() {
        String code = createAdaptiveExtensionClassCode();
        ClassLoader classLoader = findClassLoader();
        Compiler compiler = ExtensionLoader.getExtensionLoader(Compiler.class).getAdaptiveExtension();
        return compiler.compiler(code, classLoader);
    }

    /**
     * generate implication class code.
     * Necessary conditions:
     *  1、less than 1 {@link Adaptive} in interface method
     *  2、the interface method must have {@link URL} parameter type
     * @return
     */
    private String createAdaptiveExtensionClassCode() {
        StringBuilder codeBuidler = new StringBuilder();
        Method[] methods = type.getMethods();
        boolean hassAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hassAdaptiveAnnotation = true;
                break;
            }
        }
        // no need to generate adaptive class since there's no adaptive method found.
        if (!hassAdaptiveAnnotation) {
            throw new IllegalStateException("No adaptive method on extension " + type.getName() + ", refuse to create the adaptive class!");
        }

        codeBuidler.append("package " + type.getPackage().getName() + ";");
        codeBuidler.append("\nimport " + ExtensionLoader.class.getName() + ";");
        codeBuidler.append("\npublic class " + type.getSimpleName() + "$Adaptive" + " implements " + type.getCanonicalName() + " {");

        for (Method method : methods) {
            Class<?> rt = method.getReturnType();
            Class<?>[] pts = method.getParameterTypes();
            Class<?>[] ets = method.getExceptionTypes();

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            StringBuilder code = new StringBuilder(512);
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
            } else {
                int urlTypeIndex = -1;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].equals(URL.class)) {
                        urlTypeIndex = i;
                        break;
                    }
                }
                // found parameter in URL type
                if (urlTypeIndex != -1) {
                    // Null Point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"url == null\");",
                            urlTypeIndex);
                    code.append(s);

                    s = String.format("\n%s url = arg%d;", URL.class.getName(), urlTypeIndex);
                    code.append(s);
                }
                // did not find parameter in URL type
                else {
                    String attribMethod = null;

                    // find URL getter method
                    LBL_PTS:
                    for (int i = 0; i < pts.length; ++i) {
                        Method[] ms = pts[i].getMethods();
                        for (Method m : ms) {
                            String name = m.getName();
                            if ((name.startsWith("get") || name.length() > 3)
                                    && Modifier.isPublic(m.getModifiers())
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes().length == 0
                                    && m.getReturnType() == URL.class) {
                                urlTypeIndex = i;
                                attribMethod = name;
                                break LBL_PTS;
                            }
                        }
                    }
                    if (attribMethod == null) {
                        throw new IllegalStateException("fail to create adaptive class for interface " + type.getName()
                                + ": not found url parameter or url attribute in parameters of method " + method.getName());
                    }

                    // Null point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");",
                            urlTypeIndex, pts[urlTypeIndex].getName());
                    code.append(s);
                    s = String.format("\nif (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");",
                            urlTypeIndex, attribMethod, pts[urlTypeIndex].getName(), attribMethod);
                    code.append(s);

                    s = String.format("%s url = arg%d.%s();", URL.class.getName(), urlTypeIndex, attribMethod);
                    code.append(s);
                }

                String[] value = adaptiveAnnotation.value();
                // value is not set, use the value generated from class name as the key
                if (value.length == 0) {
                    char[] charArray = type.getSimpleName().toCharArray();
                    StringBuilder sb = new StringBuilder(128);
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }

                boolean hasInvocation = false;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].getName().equals("cn.ching.mandal.rpc.Invocation")) {
                        // Null Point check
                        String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"invocation == null\");", i);
                        code.append(s);
                        s = String.format("\nString methodName = arg%d.getMethodName();", i);
                        code.append(s);
                        hasInvocation = true;
                        break;
                    }
                }

                String defaultExtName = cachedDefaultName;
                String getNameCode = null;
                for (int i = value.length - 1; i >= 0; --i) {
                    if (i == value.length - 1) {
                        if (null != defaultExtName) {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                        } else {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                            else
                                getNameCode = "url.getProtocol()";
                        }
                    } else {
                        if (!"protocol".equals(value[i]))
                            if (hasInvocation)
                                getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                        else
                            getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                    }
                }
                code.append("\nString extName = ").append(getNameCode).append(";");
                // check extName == null?
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                s = String.format("\n%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
                code.append(s);

                // return statement
                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0)
                        code.append(", ");
                    code.append("arg").append(i);
                }
                code.append(");");
            }

            codeBuidler.append("\npublic " + rt.getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuidler.append(", ");
                }
                codeBuidler.append(pts[i].getCanonicalName());
                codeBuidler.append(" ");
                codeBuidler.append("arg" + i);
            }
            codeBuidler.append(")");
            if (ets.length > 0) {
                codeBuidler.append(" throws ");
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuidler.append(", ");
                    }
                    codeBuidler.append(ets[i].getCanonicalName());
                }
            }
            codeBuidler.append(" {");
            codeBuidler.append(code.toString());
            codeBuidler.append("\n}");
        }
        codeBuidler.append("\n}");
        if (logger.isDebugEnabled()) {
            logger.debug(codeBuidler.toString());
        }
        return codeBuidler.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

    public boolean hasExtension(String name) {
        if (StringUtils.isEmpty(name)){
            throw new IllegalArgumentException("Extension name is null");
        }
        try {
            return getExtensionClass(name) != null;
        }catch (Throwable t){
            return false;
        }
    }

    private Class<?> getExtensionClass(String name) {
        if (Objects.isNull(type)){
            throw new IllegalArgumentException("Extension type == null");
        }
        if (Objects.isNull(name)){
            throw new IllegalArgumentException("Extension name == null");
        }
        Class<?> clazz = getExtensionClasses().get(name);
        if (Objects.isNull(clazz)){
            throw new IllegalStateException("No such extension " + name + " for " + type.getName() + ".");
        }
        return clazz;
    }

    /**
     * return the list of extensions which already loaded.
     * @return
     */
    public Set<String> getLoadedExtension() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
    }

    /**
     * return extension instance by name.
     * @param name
     * @return
     */
    public T getLoadedExtension(String name){
        if (StringUtils.isEmpty(name)){
            throw new IllegalArgumentException("Extension name is null.");
        }
        ClassHolder<Object> holder = cachedInstances.get(name);
        if (Objects.isNull(holder)){
            cachedInstances.putIfAbsent(name, new ClassHolder<>());
            holder = cachedInstances.get(name);
        }
        return (T) holder.get();
    }
}
