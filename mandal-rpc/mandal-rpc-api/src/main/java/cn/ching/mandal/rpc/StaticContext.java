package cn.ching.mandal.rpc;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2018/3/23
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class StaticContext extends ConcurrentHashMap<Object, Object> {

    private static final long serialVersionUID = 5053279101057032350L;

    private static final String SYSTEMNAME = "system";

    private static final ConcurrentHashMap<String, StaticContext> contextMap = new ConcurrentHashMap<>();

    private String name;

    private StaticContext(String name){
        super();
        this.name = name;
    }

    public static StaticContext getSystemContext(){
        return getContext(SYSTEMNAME);
    }

    public static StaticContext getContext(String name){
        StaticContext appContext = contextMap.get(name);
        if (Objects.isNull(appContext)){
            appContext = contextMap.putIfAbsent(name, new StaticContext(name));
            if (Objects.isNull(appContext)){
                appContext = contextMap.get(name);
            }
        }
        return appContext;
    }

    public static StaticContext remove(String name){
        return contextMap.remove(name);
    }

    public static String getKey(URL url, String methodName, String suffix){
        return getKey(url.getServiceKey(), methodName, suffix);
    }

    public static String getKey(Map<String, String> params, String methodName, String suffix){
        return getKey(StringUtils.getServiceKey(params), methodName, suffix);
    }

    public static String getKey(String serviceKey, String methodName, String suffix){
        StringBuffer sb = new StringBuffer().append(serviceKey).append(".").append(methodName).append(".").append(suffix);
        return sb.toString();
    }

    public String getName() {
        return name;
    }
}
