package cn.ching.mandal.registry.integration;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Configurator;
import cn.ching.mandal.rpc.cluster.ConfiguratorFactory;
import cn.ching.mandal.rpc.cluster.directory.AbstractDirectory;

import java.util.*;

/**
 * 2018/2/6
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final ConfiguratorFactory CONFIGURATOR_FACTORY = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();

    public RegistryDirectory(URL url) {
        super(url);
    }

    @Override
    public void notify(List<URL> urls) {

    }

    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public Class<T> getInterface() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    /**
     * Convert override urls to map for use when re-refer.
     * Send all rules.
     *
     * @param urls
     *             </br>1.override://0.0.0.0/...( or override://ip:port...?anyhost=true)&para1=value1... means global rules (all of the providers take effect)
     *             </br>2.override://ip:port...?anyhost=false Special rules (only for a certain provider)
     *             </br>3.override:// rule is not supported... ,needs to be calculated by registry itself.
     *             </br>4.override://0.0.0.0/ without parameters means clearing the override
     * @return
     */
    public static List<Configurator> toConfigurators(List<URL> urls) {

        if (CollectionUtils.isEmpty(urls)){
            return Collections.emptyList();
        }

        List<Configurator> configurators = new ArrayList<>(urls.size());
        for (URL url : urls) {
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())){
                configurators.clear();
                break;
            }

            Map<String, String> overrides = new HashMap<>(url.getParameters());
            overrides.remove(Constants.ANYHOST_KEY);
            if (overrides.size() == 0){
                configurators.clear();
                continue;
            }
            configurators.add(CONFIGURATOR_FACTORY.getConfigurator(url));
        }
        Collections.sort(configurators);
        return configurators;
    }
}
