package cn.ching.mandal.config.model;

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
    private final referec methodArgTypes;
    @Getter
    private final String serviceName;
}
