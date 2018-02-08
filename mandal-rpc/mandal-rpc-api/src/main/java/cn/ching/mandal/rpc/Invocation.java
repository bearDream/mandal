package cn.ching.mandal.rpc;

import java.util.Map;

/**
 * 2018/1/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Invocation {

    /**
     * return method name
     * @return MethodName
     * @serialData
     */
    String getMethodName();

    /**
     * return parameter types
     * @return ParameterTypes
     * @serialData
     */
    Class<?>[] getParameterTypes();

    /**
     * return args
     * @return Arguments
     * @serialData
     */
    Object[] getArguments();

    /**
     * return Attachments
     * @return Attachments
     * @serialData
     */
    Map<String, String> getAttachments();

    /**
     * return attachment by key.
     *
     * @return attachment value.
     * @serialData
     */
    String getAttachment(String key);

    /**
     * return attachment by key with default value.
     *
     * @return attachment value.
     * @serialData
     */
    String getAttachment(String key, String defaultValue);

    /**
     * return attachment by key with default value.
     *
     * @return attachment value.
     * @serialData
     */
    Invoker<?> getInvoker();
}