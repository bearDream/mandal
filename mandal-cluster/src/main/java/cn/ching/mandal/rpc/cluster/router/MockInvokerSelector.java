package cn.ching.mandal.rpc.cluster.router;

import cn.ching.mandal.rpc.cluster.Router;
import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MockInvokerSelector implements Router {


    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public int compareTo(Router o) {
        return 1;
    }

    @Override
    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers, URL url, final Invocation invocation) throws RpcException {
        if (invocation.getAttachments() == null){
            return getNormalInvokers(invokers);
        }else {
            String value = invocation.getAttachments().get(Constants.INVOCATION_NEED_MOCK);
            if (Objects.isNull(value)){
                return getNormalInvokers(invokers);
            }else if (Boolean.TRUE.toString().equalsIgnoreCase(value)){
                return getMockedInvokers(invokers);
            }
        }

        return invokers;
    }

    private <T> List<Invoker<T>> getMockedInvokers(List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)){
            return invokers;
        }
        List<Invoker<T>> sInvoker = new ArrayList<>();
        invokers.stream().filter(i -> i.getUrl().getProtocol().equalsIgnoreCase(Constants.MOCK_PROTOCOL)).forEach(i -> {
            sInvoker.add(i);
        });
        return sInvoker;
    }

    /**
     * if url parameter's mock is true, then mock data. otherwise not mock.
     * @param invokers
     * @param <T>
     * @return invokers
     */
    private <T> List<Invoker<T>> getNormalInvokers(List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)){
            return invokers;
        }else {
            List<Invoker<T>> sInvokers = new ArrayList<>(invokers.size());
            invokers.stream().filter(i -> !i.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)).forEach(i -> {
                sInvokers.add(i);
            });
            return sInvokers;
        }
    }


    private <T> boolean hasMockProviders(final List<Invoker<T>> invokers){
        boolean hasMock = false;
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)){
                hasMock = true;
                break;
            }
        }
        return hasMock;
    }
}
