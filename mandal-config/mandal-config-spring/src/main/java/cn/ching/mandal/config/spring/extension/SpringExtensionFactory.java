package cn.ching.mandal.config.spring.extension;

import cn.ching.mandal.common.extension.ExtensionFactory;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import org.springframework.context.ApplicationContext;

import java.util.Set;

/**
 * 2018/3/8
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class SpringExtensionFactory implements ExtensionFactory {

    private static final Set<ApplicationContext> CONTEXTS = new ConcurrentHashSet<>();

    public static void addApplicationContext(ApplicationContext context){
        CONTEXTS.add(context);
    }

    public static void removeApplicationContext(ApplicationContext context){
        CONTEXTS.remove(context);
    }

    @Override
    public <T> T getExtension(Class<T> type, String name) {
        for (ApplicationContext context : CONTEXTS) {
            if (context.containsBean(name)){
                Object bean = context.getBean(name);
                if (type.isInstance(bean)){
                    return (T) bean;
                }
            }
        }
        return null;
    }
}
