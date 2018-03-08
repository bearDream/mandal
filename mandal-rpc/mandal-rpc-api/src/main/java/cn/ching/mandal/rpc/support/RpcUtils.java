package cn.ching.mandal.rpc.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ReflectUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.RpcInvocation;
import cn.ching.mandal.rpc.RpcResult;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 2018/1/13
 * utils for rpc
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RpcUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcUtils.class);

    private static final AtomicLong INVOKER_ID = new AtomicLong(0L);

    /**
     * get return type for invocation
     * @param invocation
     * @return
     */
    public static Class<?> getReturnTypes(Invocation invocation){
        try {
            if (Objects.nonNull(invocation)
                    && Objects.nonNull(invocation.getInvoker())
                    && Objects.nonNull(invocation.getInvoker().getUrl())
                    && invocation.getMethodName().startsWith("$")){
                String service = invocation.getInvoker().getUrl().getServiceInterface();
                if (Objects.nonNull(service) && service.length() > 0){
                    Class<?> clazz = ReflectUtils.name2class(service);
                    Method method = clazz.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                    if (method.getReturnType() == void.class){
                        return null;
                    }
                    return method.getReturnType();
                }
            }
        }catch (Exception e){
            LOGGER.warn(e.getMessage(), e);
        }
        return null;
    }

    public static Long getInvocationId(Invocation invocation){
        String id = invocation.getAttachment(Constants.ID_KEY);
        return Objects.isNull(id) ? null : new Long(id);
    }

    public static void attachInvokeIdIfAsync(URL url, Invocation invocation) {
        if (isAttachInvocationId(url, invocation) && Objects.isNull(getInvocationId(invocation)) && invocation instanceof  RpcInvocation){
            ((RpcInvocation) invocation).setAttachment(Constants.ID_KEY, String.valueOf(INVOKER_ID.getAndIncrement()));
        }
    }

    /**
     * if method name is "$invoke" and the first parameter type is String then return methodName is the first parameter name
     * otherwise return invocation methodName
     * @param invocation
     * @return
     */
    public static String getMethodName(Invocation invocation) {
        if (Constants.$INVOKE.equals(invocation.getMethodName())
                && Objects.nonNull(invocation.getArguments())
                && invocation.getArguments().length > 0
                && invocation.getArguments()[0] instanceof String){

            return (String) invocation.getArguments()[0];
        }
        return invocation.getMethodName();
    }

    private static boolean isAttachInvocationId(URL url, Invocation invocation){
        String val = url.getMethodParameter(invocation.getMethodName(), Constants.AUTO_ATTACH_INVOCATIONID_KEY);
        if (Objects.isNull(val)){
            return isAsync(url, invocation);
        }else if (Boolean.TRUE.toString().equalsIgnoreCase(val)){
            return true;
        }else {
            return false;
        }
    }

    private static boolean isAsync(URL url, Invocation invocation) {
        if (Boolean.TRUE.toString().equalsIgnoreCase(invocation.getAttachment(Constants.ASYNC_KEY))){
            return true;
        }else{
            return url.getMethodParameter(getMethodName(invocation), Constants.ASYNC_KEY, false);
        }
    }

    /**
     * if this object is RpcException then return code, otherwise return 0
     * @param t RpcResult or other object
     * @return {@link RpcException#getCode()}
     */
    public static int convertExceptionCode(Object t){
        return t instanceof RpcException ? ((RpcException) t).getCode() : 0;
    }

    public static Object[] getArgument(Invocation invocation) {
        if (Constants.$INVOKE.equals(invocation.getMethodName())
                && !Objects.isNull(invocation.getArguments())
                && invocation.getArguments().length > 1
                && invocation.getArguments()[1] instanceof String[]){
            String[] types = (String[]) invocation.getArguments()[1];
            if (Objects.isNull(types)){
                return new Class<?>[0];
            }
            Class<?>[] parameterTypes = new Class<?>[types.length];
            for (int i = 0; i < types.length; i++) {
                parameterTypes[i] = ReflectUtils.forName(types[i]);
            }
            return parameterTypes;
        }
        return invocation.getParameterTypes();
    }
}
