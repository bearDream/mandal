package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.support.ProtocolUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2018/1/11
 * Protocol template
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractProtocol implements Protocol{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<>();

    protected static String serviceKey(URL url){
        return ProtocolUtils.serviceKey(url);
    }

    protected static String serviceKey(int port, String serviceName, String servicecVersion, String serviceGroup){
        return ProtocolUtils.serviceKey(port, serviceName, servicecVersion, serviceGroup);
    }

    @Override
    public void destroy() {
        // destroy invoker
        invokers.stream().filter(o -> Objects.nonNull(o)).forEach(invoker -> {
            invokers.remove(invoker);
            try {
                if (logger.isInfoEnabled()){
                    logger.info("destroy refrence " + invoker.getUrl());
                }
                invoker.destroy();
            }catch (Throwable e){
                logger.warn(e.getMessage(), e);
            }
        });
        // destroy exporter
        exporterMap.keySet().stream().forEach(e -> {
            Exporter<?> exporter = exporterMap.remove(e);
            if (Objects.nonNull(exporter)){
                try {
                    if (logger.isInfoEnabled()){
                        logger.warn("Unexport service:" + exporter.getInvoker().getUrl());
                    }
                }catch (Throwable t){
                    logger.warn(t.getMessage(), t);
                }
            }
        });
    }
}
