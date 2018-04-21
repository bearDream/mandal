package cn.ching.mandal.registry.mandal;

import cn.ching.mandal.cluster.Cluster;
import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.bytecode.Wrapper;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.registry.RegistryService;
import cn.ching.mandal.registry.integration.RegistryDirectory;
import cn.ching.mandal.registry.support.AbstractRegistryFactory;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.ProxyFactory;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * 2018/4/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalRegistryFactory extends AbstractRegistryFactory{

    @Setter
    private Protocol protocol;
    @Setter
    private ProxyFactory proxyFactory;
    @Setter
    private Cluster cluster;

    @Override
    protected Registry createRegistry(URL url) {

        url = getRegistryUrl(url);
        List<URL> urls = new ArrayList<>();
        urls.add(url.removeParameter(Constants.BACKUP_KEY));
        String backup = url.getParameter(Constants.BACKUP_KEY);
        if (!StringUtils.isEmpty(backup)){
            String[] address = Constants.COMMA_SEPARATOR.split(backup);
            for (String a : address) {
                urls.add(url.setAddress(a));
            }
        }
        RegistryDirectory<RegistryService> directory = new RegistryDirectory<>(RegistryService.class, url.addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName()).addParameterAndEncoded(Constants.REFER_KEY, url.toParameterString()));
        Invoker<RegistryService> registryInvoker = cluster.join(directory);
        RegistryService registryService = proxyFactory.getProxy(registryInvoker);

        MandalRegistry registry = new MandalRegistry(registryInvoker, registryService);
        return registry;
    }

    private static URL getRegistryUrl(URL url){
        return url.setPath(RegistryService.class.getName())
                .removeParameter(Constants.EXPORT_KEY)
                .removeParameter(Constants.REFER_KEY)
                .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName())
                .addParameter(Constants.CLUSTER_STICKY_KEY, "true")
                .addParameter(Constants.LAZY_CONNECT_KEY, "true")
                .addParameter(Constants.RECONNECT_KEY, "false")
                .addParameterIfAbsent(Constants.TIMEOUT_KEY, "10000")
                .addParameterIfAbsent(Constants.CALLBACK_INSTANCES_LIMIT_KEY, "10000")
                .addParameterIfAbsent(Constants.CONNECT_TIMEOUT_KEY, "10000")
                .addParameterIfAbsent(Constants.METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(Wrapper.getWrapper(RegistryService.class).getDeclaredMethodNames())),","))
                .addParameter("subscribe.1.callback", "true")
                .addParameter("unsubscribe.1.callback", "false");
    }
}
