package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Invoker;

import java.util.Objects;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractExporter<T> implements Exporter<T>{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Invoker<T> invoker;

    private volatile boolean unexported = false;

    public AbstractExporter(Invoker<T> invoker){
        if (Objects.isNull(invoker)){
            throw new IllegalStateException("service invoker is null");
        }
        if (Objects.isNull(invoker.getInterface())){
            throw new IllegalStateException("service type is null");
        }
        if (Objects.isNull(invoker.getUrl())){
            throw new IllegalStateException("service url is null");
        }
        this.invoker = invoker;
    }

    @Override
    public Invoker<T> getInvoker() {
        return invoker;
    }

    @Override
    public void unexport() {
        if (unexported){
            return;
        }
        unexported = true;
        getInvoker().destroy();
    }

    @Override
    public String toString() {
        return getInvoker().toString();
    }
}
