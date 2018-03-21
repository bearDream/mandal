package cn.ching.mandal.common.serialize.support.kryo.utils;

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
