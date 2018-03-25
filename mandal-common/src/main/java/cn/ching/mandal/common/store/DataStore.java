package cn.ching.mandal.common.store;

import cn.ching.mandal.common.extension.SPI;

import java.util.Map;

/**
 * 2018/3/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("simple")
public interface DataStore {

    Map<String, Object> get(String componentName);

    Object get(String componentName, String key);

    void put(String componentName, String key, Object value);

    void remove(String componentName, String key);
}
