package cn.ching.mandal.config.model;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.config.ReferenceConfig;
import lombok.Getter;

import java.lang.reflect.Method;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConsumerMethodModel {

    @Getter
    private transient final Method method;
    @Getter
    private final String methodName;
    @Getter
    private final ReferenceConfig metadata;
    @Getter
    private final String[] parameterTypes;
    @Getter
    private final Class<?>[] parameterClasses;
    @Getter
    private final Class<?> returnClass;
    @Getter
    private final boolean generic;

    public ConsumerMethodModel(Method method, ReferenceConfig metadata) {
        this.method = method;
        this.parameterClasses = method.getParameterTypes();
        this.returnClass = method.getReturnType();
        this.parameterTypes = this.createParamSignature(parameterClasses);
        this.methodName = method.getName();
        this.metadata = metadata;
        this.generic = methodName.equals(Constants.$INVOKE) && parameterTypes != null && parameterTypes.length == 3;
    }

    private String[] createParamSignature(Class<?>[] args) {
        if (args == null || args.length == 0){
            return new String[]{};
        }
        String[] paramSig = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            paramSig[i] = args[i].getName();
        }
        return paramSig;
    }
}
