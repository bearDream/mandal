package cn.ching.mandal.rpc.injvm;

import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.protocol.AbstractExporter;

import java.util.Map;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class InjvmExporter<T> extends AbstractExporter<T> {

    private final String key;

    private final Map<String, Exporter<?>> exporterMap;

    public InjvmExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
        super(invoker);
        this.key = key;
        this.exporterMap = exporterMap;
        exporterMap.put(key, this);
    }

    public void unexporter() {
        super.unexport();
        exporterMap .remove(key);
    }
}
