package cn.ching.mandal.common.serialize.support.kryo.utils;

import com.esotericsoftware.kryo.Kryo;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class KryoUtil {

    private static AbstractKryoFactory kryoFactory = new ThreadLocalKryoFactory();

    public static Kryo get(){
        return kryoFactory.getKryo();
    }

    public static void release(Kryo kryo){
        kryoFactory.returnKryo(kryo);
    }

    public static void register(Class<?> clazz){
        kryoFactory.registerClass(clazz);
    }

    public static void setRegistrationRequired(boolean registrationRequired){
        kryoFactory.setRegistrationRequired(registrationRequired);
    }
}
