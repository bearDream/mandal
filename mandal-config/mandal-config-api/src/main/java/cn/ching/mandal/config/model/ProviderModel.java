package cn.ching.mandal.config.model;

import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.config.ServiceConfig;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProviderModel {

    @Getter
    private final String serviceName;
    @Getter
    private final Object serviceInstance;
    @Getter
    private final ServiceConfig metadata;
    private final Map<String, List<ProviderMethodModel>> methods = new HashMap<>();

    public ProviderModel(String serviceName, ServiceConfig metadata, Object serviceInstance){
        if (Objects.isNull(serviceInstance)){
            throw new IllegalArgumentException("Service [" + serviceName + "] Target is null.");
        }
        this.serviceInstance = serviceInstance;
        this.serviceName = serviceName;
        this.metadata = metadata;

        initMethod();
    }

    public List<ProviderMethodModel> getAllMethods(){
        List<ProviderMethodModel> result = new ArrayList<>();
        methods.values().forEach(m -> {
            result.addAll(m);
        });
        return result;
    }

    public ProviderMethodModel getMethodModels(String methodName, String[] argType){
        List<ProviderMethodModel> methodModels = methods.get(methodName);
        if (!Objects.isNull(methodModels)){
            methodModels = methodModels
                    .stream()
                    .filter(m -> Arrays.equals(argType, m.getMethodArgTypes()))
                    .collect(Collectors.toList());
            return CollectionUtils.isEmpty(methodModels) ? null : methodModels.get(0);
        }
        return null;
    }

    private void initMethod() {
        Method[] methodsToExport = null;
        methodsToExport = metadata.getInterfaceClass().getMethods();

        for (Method method : methodsToExport) {
            method.setAccessible(true);

            List<ProviderMethodModel> methodModels = methods.get(method.getName());
            if (Objects.isNull(methodModels)){
                methodModels = new ArrayList<ProviderMethodModel>(1);
                methods.put(method.getName(), methodModels);
            }
            methodModels.add(new ProviderMethodModel(method, serviceName));
        }
    }
}
