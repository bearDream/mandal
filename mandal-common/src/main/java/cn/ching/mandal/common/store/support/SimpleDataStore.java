package cn.ching.mandal.common.store.support;

import cn.ching.mandal.common.store.DataStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 2018/3/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class SimpleDataStore implements DataStore {

    private  ConcurrentMap<String, ConcurrentMap<String, Object>> store = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> get(String componentName) {
        ConcurrentMap<String, Object> value = store.get(componentName);
        if (value == null){
            return new HashMap<>();
        }
        return value;
    }

    @Override
    public Object get(String componentName, String key) {
        if (!store.containsKey(componentName)){
            return null;
        }
        return store.get(componentName).get(key);
    }

    @Override
    public void put(String componentName, String key, Object value) {
        Map<String, Object> componentData = store.get(componentName);
        if (componentData == null){
            store.putIfAbsent(componentName, new ConcurrentHashMap<>());
            componentData = store.get(componentName);
        }
        componentData.put(key, value);
    }

    @Override
    public void remove(String componentName, String key) {
        if (!store.containsKey(componentName)){
            return;
        }
        store.remove(componentName);
    }
}
