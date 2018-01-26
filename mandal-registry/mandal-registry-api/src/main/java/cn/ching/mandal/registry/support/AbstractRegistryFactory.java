package cn.ching.mandal.registry.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.registry.RegistryFactory;
import cn.ching.mandal.registry.RegistryService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 2018/1/17
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractRegistryFactory implements RegistryFactory{

    private static final Logger logger = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    private static final Lock LOCK = new ReentrantLock();

    private static final Map<String, Registry> REGISTRIES = new ConcurrentHashMap<>();

    public static Collection<Registry> getRegistries(){
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    public static void destroyAll(){
        if (logger.isInfoEnabled()){
            logger.info("close all registries." + getRegistries());
        }
        LOCK.lock();
        try {
            getRegistries().forEach(registry -> {
                try {
                    registry.destroy();
                }catch (Throwable t){
                    logger.error("error in destry registr cause:"+t.getMessage(), t);
                }
            });
            REGISTRIES.clear();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public Registry getRegistry(URL url) {

        url = url.setPath(RegistryService.class.getName())
                .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName())
                .removeParameters(Constants.EXPORT_KEY, Constants.REFER_KEY);
        String key = url.toServiceString();
        LOCK.lock();
        try {
            Registry registry = REGISTRIES.get(key);
            if (!Objects.isNull(registry)){
                return registry;
            }
            registry = createRegistry(url);
            if (Objects.isNull(registry)){
                throw new IllegalStateException("can't create registry. url: " + url);
            }
            REGISTRIES.put(key, registry);
            return registry;
        }finally {
            LOCK.unlock();
        }
    }

    protected abstract Registry createRegistry(URL url);
}
