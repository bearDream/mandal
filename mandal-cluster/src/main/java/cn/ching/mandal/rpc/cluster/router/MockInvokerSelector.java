package cn.ching.mandal.rpc.cluster.router;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Router;

import java.util.List;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MockInvokerSelector implements Router{
    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public int compareTo(Router o) {
        return 0;
    }
}
