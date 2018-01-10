package cn.ching.mandal.common.utils;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2018/1/9
 * use ConcurrentHashMap impl ConcurrentHashSet(like dubbo ConcurrentHashSet)
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConcurrentHashSet<T> extends AbstractSet<T> implements Set<T>, Serializable {

    private static final long serialVersionUID = -2624819438081423742L;

    private static final Object PRESENT = new Object();

    public final ConcurrentHashMap<T, Object> map;

    public ConcurrentHashSet(){
        map = new ConcurrentHashMap<T, Object>();
    }

    public ConcurrentHashSet(int initCapacity){
        map = new ConcurrentHashMap<T, Object>(initCapacity);
    }

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(T t) {
        return map.put(t, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }
}
