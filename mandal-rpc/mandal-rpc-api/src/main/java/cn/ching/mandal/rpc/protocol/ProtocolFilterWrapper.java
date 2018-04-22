package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.rpc.*;

import java.util.List;
import java.util.Objects;

/**
 * 2018/1/11
 * build all filter(it is use Decorator pattern)
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProtocolFilterWrapper implements Protocol{

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol){
        if (Objects.isNull(protocol)){
            throw new IllegalArgumentException("Error: protocol is null");
        }
        this.protocol = protocol;
    }

    /**
     * build all activate filter
     * <p>
     * actually, buildInvokerChain can build all activate filter and invoke it.
     * For example(assume system has two filters:AccessLogFilter, TokenFilter):
     * when build over, the actually invoke sequence like it:
     * AccessLogFilter -> TokenFilter -> actually invoker
     * </p>
     * @param invoker
     * @param key
     * @param group
     * @param <T>
     * @return
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group){
        Invoker<T> last = invoker;
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
        if (filters.size() > 0){
            for (int i=filters.size()-1; i >= 0; i--){
                Filter filter = filters.get(i);
                Invoker<T> next = last;
                last = new Invoker<T>() {
                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public Result invoke(Invocation invocation) {
                        return filter.invoker(next, invocation);
                    }

                    @Override
                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    @Override
                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    @Override
                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())){
            return protocol.export(invoker);
        }
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())){
            return protocol.refer(type, url);
        }
        return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }
}
