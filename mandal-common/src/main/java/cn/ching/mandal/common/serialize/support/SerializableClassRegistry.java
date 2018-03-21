package cn.ching.mandal.common.serialize.support;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class SerializableClassRegistry {

    private static final Set<Class> registrations = new LinkedHashSet<>();

    public static void registerClass(Class clazz){
        registrations.add(clazz);
    }

    public static Set<Class> getRegisteredClasses(){
        return registrations;
    }
}
