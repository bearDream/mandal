package cn.ching.mandal.config.model;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.rpc.Invoker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ApplicationModel {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationModel.class);

    private static final ConcurrentMap<String, ConsumerModel> consumerdService = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, ProviderModel> providerdService = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Set<Invoker>> providerServiceInvoker = new ConcurrentHashMap<>();

    public static List<ConsumerModel> allConsumerModel(){
        return new ArrayList<ConsumerModel>(consumerdService.values());
    }

    public static List<ProviderModel> allProviderModel(){
        return new ArrayList<ProviderModel>(providerdService.values());
    }

    public static ProviderModel getProviderdService(String serviceName) {
        return providerdService.get(serviceName);
    }

    public static ConsumerModel getConsumerdService(String serviceName) {
        return consumerdService.get(serviceName);
    }

    public static boolean initConsumerModel(String serviceName, ConsumerModel consumerModel){
        if (consumerdService.putIfAbsent(serviceName, consumerModel) != null){
            logger.warn("consumerModel already exits.");
            return false;
        }
        return true;
    }

    public static boolean initProviderModel(String serviceName, ProviderModel providerModel){
        if (providerdService.putIfAbsent(serviceName, providerModel) != null){
            logger.warn("providerModel already exits.");
            return false;
        }
        return true;
    }

    public static void addProviderInvoker(String serviceName, Invoker invoker){
        Set<Invoker> invokers = providerServiceInvoker.get(serviceName);
        if (Objects.isNull(invokers)){
            providerServiceInvoker.putIfAbsent(serviceName, new ConcurrentHashSet<Invoker>());
            invokers = providerServiceInvoker.get(serviceName);
        }
        invokers.add(invoker);
    }

    public static Set<Invoker> getProviderInvoker(String serviceName){
        Set<Invoker> invokers = providerServiceInvoker.get(serviceName);
        if (Objects.isNull(invokers)){
            return Collections.emptySet();
        }
        return invokers;
    }

}
