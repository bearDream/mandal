package cn.ching.mandal.rpc.injvm;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.protocol.AbstractInvoker;

import java.util.Map;
import java.util.Objects;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class InjvmInvoker<T> extends AbstractInvoker<T> {

    private final String key;

    private final Map<String, Exporter<?>> exporterMap;

    public InjvmInvoker(Class<T> type, URL url, String key, Map<String, Exporter<?>> exporterMap) {
        super(type, url);
        this.key = key;
        this.exporterMap = exporterMap;
    }

    @Override
    public boolean isAvailable() {
        InjvmExporter<?> exporter = (InjvmExporter<?>) exporterMap.get(key);
        if (Objects.isNull(exporter)){
            return false;
        }else {
            return super.isAvailable();
        }
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        Exporter<?> exporter = InjvmProtocol.getExporter(exporterMap, getUrl());
        if (Objects.isNull(exporter)){
            throw new RpcException("service [ " + key + " ] not found");
        }
        RpcContext.getContext().setRemoteAddress(NetUtils.LOCALHOST, 0);
        return exporter.getInvoker().invoke(invocation);
    }
}
