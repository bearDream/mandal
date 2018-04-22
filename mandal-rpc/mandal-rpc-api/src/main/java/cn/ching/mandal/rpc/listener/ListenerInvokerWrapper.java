package cn.ching.mandal.rpc.listener;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.InvokerListener;
import cn.ching.mandal.rpc.Result;

import java.util.List;
import java.util.Objects;

/**
 * 2018/4/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ListenerInvokerWrapper<T> implements Invoker<T>{

    private static final Logger logger = LoggerFactory.getLogger(ListenerInvokerWrapper.class);

    private final Invoker<T> invoker;

    private final List<InvokerListener> listeners;


    public ListenerInvokerWrapper(Invoker<T> invoker, List<InvokerListener> listeners){

        if (Objects.isNull(invoker)){
            throw new IllegalArgumentException("Invoker is null!");
        }
        this.invoker = invoker;
        this.listeners = listeners;
        if (!CollectionUtils.isEmpty(listeners)){
            listeners.stream()
                    .filter(listener -> !Objects.isNull(listener))
                    .forEach(listener -> {
                        try {
                            listener.referred(invoker);
                        }catch (Throwable t){
                            logger.error(t.getMessage(), t);
                        }
                    });
        }
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) {
        return invoker.invoke(invocation);
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public void destroy() {
        try {
            invoker.destroy();
        }finally {
            if (!CollectionUtils.isEmpty(listeners)){
                listeners.stream()
                        .filter(listener -> !Objects.isNull(listener))
                        .forEach(listener -> {
                            try {
                                listener.destroyed(invoker);
                            }catch (Throwable t){
                                logger.error(t.getMessage(), t);
                            }
                        });
            }
        }
    }

    @Override
    public String toString() {
        return getInterface() + " is -->> " + (Objects.isNull(getUrl()) ? " " : getUrl().toString());
    }
}
