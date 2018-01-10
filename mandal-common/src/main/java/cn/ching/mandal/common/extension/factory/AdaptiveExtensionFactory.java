package cn.ching.mandal.common.extension.factory;

import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.ExtensionFactory;
import cn.ching.mandal.common.extension.ExtensionLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 2018/1/10
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Adaptive
public class AdaptiveExtensionFactory implements ExtensionFactory{

    private final List<ExtensionFactory> factories;

    public AdaptiveExtensionFactory(){
        ExtensionLoader<ExtensionFactory> loader = ExtensionLoader.getExtensionLoader(ExtensionFactory.class);
        List<ExtensionFactory> list = new ArrayList<>();
        for (String name : loader.getSupportedExtensions()){
            list.add(loader.getExtension(name));
        }
        factories = Collections.unmodifiableList(list);
    }


    @Override
    public <T> T getExtension(Class<T> type, String name) {
        for (ExtensionFactory factory : factories){
            T extension = factory.getExtension(type, name);
            if (Objects.nonNull(extension)){
                return extension;
            }
        }
        return null;
    }
}
