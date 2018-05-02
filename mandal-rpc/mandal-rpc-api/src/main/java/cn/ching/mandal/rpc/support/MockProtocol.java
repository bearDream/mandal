package cn.ching.mandal.rpc.support;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.protocol.AbstractProtocol;

/**
 * 2018/3/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MockProtocol extends AbstractProtocol {

    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        return new MockInvoker<T>(url);
    }
}
