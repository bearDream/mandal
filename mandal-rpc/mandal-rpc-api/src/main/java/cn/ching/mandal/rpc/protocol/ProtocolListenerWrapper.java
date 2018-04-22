package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.listener.ListenerExportWrapper;
import cn.ching.mandal.rpc.listener.ListenerInvokerWrapper;

import java.util.Collections;
import java.util.Objects;

/**
 * 2018/3/25
 * Protocol --> ProtocolListenerWrapper --> ListenerInvokerWrapper / ListenerExporterWrapper /  --> InvokerListener / ExporterListener
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProtocolListenerWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolListenerWrapper(Protocol protocol){
        if (Objects.isNull(protocol)){
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
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
        return new ListenerExportWrapper<T>(protocol.export(invoker),
                Collections.unmodifiableList(
                        ExtensionLoader.getExtensionLoader(ExporterListener.class)
                                .getActivateExtension(invoker.getUrl(), Constants.EXPORTER_LISTENER_KEY)
                ));

    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())){
            return protocol.refer(type, url);
        }

        return new ListenerInvokerWrapper<T>(protocol.refer(type, url),
                    Collections.unmodifiableList(
                            ExtensionLoader.getExtensionLoader(InvokerListener.class)
                            .getActivateExtension(url, Constants.INVOKER_LISTENER_KEY)
                    )
                );
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }
}
