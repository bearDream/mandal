package cn.ching.mandal.common.serialize.support.kryo.utils;

import cn.ching.mandal.common.utils.StringUtils;

import java.lang.reflect.Method;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class ReflectionUtils {

    public static boolean checkZeroArgConstructor(Class clazz){
        try {
            clazz.getDeclaredConstructor(clazz);
            return true;
        }catch (NoSuchMethodException e){
            return false;
        }
    }
}
