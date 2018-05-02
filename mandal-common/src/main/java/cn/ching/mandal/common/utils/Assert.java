package cn.ching.mandal.common.utils;

import java.util.Objects;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class Assert {

    protected Assert(){

    }

    public static void notNull(Object obj, String messsage){
        if (Objects.isNull(obj)){
            throw new IllegalArgumentException(messsage);
        }
    }

    public static void notNull(Object obj, RuntimeException exception){
        if (Objects.isNull(obj)){
            throw exception;
        }
    }
}
