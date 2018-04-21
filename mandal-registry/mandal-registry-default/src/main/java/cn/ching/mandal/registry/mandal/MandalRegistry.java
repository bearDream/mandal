package cn.ching.mandal.registry.mandal;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.registry.NotifyListener;
import cn.ching.mandal.registry.RegistryService;
import cn.ching.mandal.registry.support.FailbackRegistry;
import cn.ching.mandal.rpc.Invoker;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 2018/4/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MandalRegistry.class);

    // reconnecting cycle: 3 seconds
    private static final int RECONNECT_PERIOD_DEFAULT = 3 * 1000;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("MandalRegistryReconnectTimer", true));

    private final ReentrantLock clientLock = new ReentrantLock();

    private final ScheduledFuture<?> reconnectFuture;

    private final Invoker<RegistryService> registryInvoker;

    private final RegistryService registryService;

    public MandalRegistry(Invoker<RegistryService> registryInvoker, RegistryService registryService) {
        super(registryInvoker.getUrl());

        this.registryInvoker = registryInvoker;
        this.registryService = registryService;

        int reconnectPeriod = registryInvoker.getUrl().getParameter(Constants.REGISTRY_RECONNECT_PERIOD_KEY, RECONNECT_PERIOD_DEFAULT);
        reconnectFuture = executorService.scheduleWithFixedDelay(() -> {
            try {
                connect();
            }catch (Throwable t){
                logger.error("unexpected error occur at reconnect, cause: " + t.getMessage(), t);
            }
        }, reconnectPeriod, reconnectPeriod, TimeUnit.MILLISECONDS);
    }

    protected final void connect() {
        try {
            if (isAvailable()){
                return;
            }
            if (logger.isInfoEnabled()){
                logger.info("Reconnect to registry " + getUrl());
            }
            clientLock.lock();
            try {
                if (isAvailable()){
                    return;
                }
                // reconnect.
                recover();
            }finally {
                clientLock.unlock();
            }
        }catch (Throwable t){
            if (getUrl().getParameter(Constants.CHECK_KEY, true)){
                if (t instanceof RuntimeException){
                    throw (RuntimeException) t;
                }
                throw new RuntimeException(t.getMessage(), t);
            }
            logger.error("Failed to connect to registry " + getUrl().getAddress() + " from provider/consumer " + NetUtils.getLocalHost() + " use mandal " + Version.getVersion() + ", cause by:" + t.getMessage(), t);
        }
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        registryService.subscribe(url, listener);
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        registryService.unsubscribe(url, listener);
    }

    @Override
    protected void doRegister(URL url) {
        registryService.register(url);
    }

    @Override
    protected void doUnRegister(URL url) {
        registryService.unregister(url);
    }

    @Override
    public boolean isAvailable() {
        if (registryInvoker == null){
            return false;
        }
        return registryInvoker.isAvailable();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            if (!reconnectFuture.isCancelled()){
                reconnectFuture.cancel(true);
            }
        }catch (Throwable t){
            logger.warn("Failed cancel reconnect.", t);
        }
    }
}
