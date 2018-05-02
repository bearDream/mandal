package cn.ching.mandal.rpc.listener;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.ExporterListener;
import cn.ching.mandal.rpc.Invoker;

import java.util.List;
import java.util.Objects;

/**
 * 2018/4/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ListenerExportWrapper<T> implements Exporter<T> {

    private static final Logger logger = LoggerFactory.getLogger(ListenerExportWrapper.class);

    private final Exporter<T> exporter;

    private final List<ExporterListener> listeners;

    public ListenerExportWrapper(Exporter<T> exporter, List<ExporterListener> listeners){
        if (Objects.isNull(exporter)){
            throw new IllegalArgumentException("exporter is null!");
        }

        this.exporter = exporter;
        this.listeners = listeners;

        if (!CollectionUtils.isEmpty(listeners)){
            RuntimeException exception = null;
            for (ExporterListener listener : listeners) {
                if (!Objects.isNull(listener)){
                    try {
                        listener.exporter(this);
                    }catch (RuntimeException e){
                        logger.error(e.getMessage(), e);
                        exception = e;
                    }
                }
            }
            if (!Objects.isNull(exception)){
                throw exception;
            }
        }
    }

    @Override
    public Invoker<T> getInvoker() {
        return exporter.getInvoker();
    }

    @Override
    public void unexport() {
        try {
            exporter.unexport();
        }finally {
            if (!Objects.isNull(listeners) && !CollectionUtils.isEmpty(listeners)){
                RuntimeException exception = null;
                for (ExporterListener listener : listeners) {
                    if (!Objects.isNull(listener)){
                        try {
                            listener.unexporter(this);
                        }catch (RuntimeException e){
                            logger.error(e.getMessage(), e);
                            exception = e;
                        }
                    }
                }
                if (!Objects.isNull(exception)){
                    throw exception;
                }
            }
        }
    }
}
