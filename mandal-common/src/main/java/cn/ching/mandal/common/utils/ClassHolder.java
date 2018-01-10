package cn.ching.mandal.common.utils;

/**
 * 2018/1/5
 * help class hold a value
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ClassHolder<T> {

    private volatile T value;

    public T get() {
        return value;
    }

    public void set(T value){
        this.value=value;
    }
}
