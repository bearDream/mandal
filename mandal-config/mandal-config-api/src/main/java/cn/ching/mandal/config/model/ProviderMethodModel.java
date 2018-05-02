package cn.ching.mandal.config.model;

import lombok.Getter;

import java.lang.reflect.Method;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProviderMethodModel {

    @Getter
    private transient final Method method;
    @Getter
    private final String methodName;
    @Getter
    private final String[] methodArgTypes;
    @Getter
    private final String serviceName;

    public ProviderMethodModel(Method method, String serviceName){
        this.method = method;
        this.serviceName = serviceName;
        this.methodName = method.getName();
        this.methodArgTypes = getArgTypes(method);
    }

    private String[] getArgTypes(Method method) {
        String[] methodArgTypes = new String[0];
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0){
            methodArgTypes = new String[parameterTypes.length];
            int index = 0;
            for (Class<?> parameterType : parameterTypes) {
                methodArgTypes[index++] = parameterType.getName();
            }
        }
        return methodArgTypes;
    }
}
