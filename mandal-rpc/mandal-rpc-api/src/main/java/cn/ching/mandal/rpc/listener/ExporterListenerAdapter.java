package cn.ching.mandal.rpc.listener;

import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.ExporterListener;
import cn.ching.mandal.rpc.RpcException;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class ExporterListenerAdapter implements ExporterListener{

    @Override
    public void exporter(Exporter<?> exporter) throws RpcException {
    }

    @Override
    public void unexporter(Exporter<?> exporter) throws RpcException {
    }
}
