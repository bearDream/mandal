package cn.ching.mandal.common.extension.factory;

import cn.ching.mandal.common.extension.ExtensionFactory;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/10
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class SpiExtensionFactory implements ExtensionFactory{

    @Override
    public <T> T getExtension(Class<T> type, String name) {
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)){
            ExtensionLoader loader = ExtensionLoader.getExtensionLoader(type);
            if (loader.getSupportedExtensions().size() > 0){
                return (T) loader.getAdaptiveExtension();
            }
        }
        return null;
    }
}
