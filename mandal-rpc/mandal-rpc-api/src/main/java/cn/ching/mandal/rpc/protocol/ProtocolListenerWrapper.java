package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.RpcException;

/**
 * 2018/3/25
 * todo
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProtocolListenerWrapper implements Protocol {
    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return null;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        return null;
    }

    @Override
    public void destroy() {

    }
}
