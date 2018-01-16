package cn.ching.mandal.rpc;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 2018/1/12
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RpcInvocation implements Invocation, Serializable {

    private static final long serialVersionUID = 2395215182991618238L;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] arguments;

    private Map<String, String> attachments;

    private transient Invoker<?> invoker;

    public RpcInvocation(){

    }

    public RpcInvocation(Invocation invocation, Invoker<?> invoker){
        this(invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments(), new HashMap<>(invocation.getAttachments()), invocation.getInvoker());
        if (Objects.nonNull(invoker)){
            URL url = invoker.getUrl();
            setAttachment(Constants.PATH_KEY, url.getPath());
            if (url.hasParameter(Constants.INTERFACE_KEY)){
                setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            }
            if (url.hasParameter(Constants.GROUP_KEY)){
                setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            }
            if (url.hasParameter(Constants.VERSION_KEY)){
                setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            }
            if (url.hasParameter(Constants.TOKEN_KEY)){
                setAttachment(Constants.TOKEN_KEY, url.getParameter(Constants.TOKEN_KEY));
            }
            if (url.hasParameter(Constants.TIMEOUT_KEY)){
                setAttachment(Constants.TIMEOUT_KEY, url.getParameter(Constants.TIMEOUT_KEY));
            }
            if (url.hasParameter(Constants.APPLICATION_KEY)){
                setAttachment(Constants.APPLICATION_KEY, url.getParameter(Constants.APPLICATION_KEY));
            }
        }
    }

    public RpcInvocation(Invocation invocation){
        this(invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments(), invocation.getAttachments(), invocation.getInvoker());
    }

    public RpcInvocation(Method method, Object[] arguments){
        this(method.getName(), method.getParameterTypes(), arguments, null, null);
    }

    public RpcInvocation(Method method, Object[] arguments, Map<String, String> attachments){
        this(method.getName(), method.getParameterTypes(), arguments, attachments, null);
    }

    public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments){
        this(methodName, parameterTypes, arguments, null, null);
    }

    public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments, Map<String, String> attachments){
        this(methodName, parameterTypes, arguments, attachments, null);
    }

    public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments, Map<String, String> attachments, Invoker<?> invoker){
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;
        this.attachments = attachments;
        this.invoker = invoker;
    }

    public void setAttachment(String pathKey, String path) {
        if (Objects.isNull(attachments)){
            attachments = new HashMap<>();
        }
        attachments.put(pathKey, path);
    }

    public void setAttachmentIfAbsent(String key, String value) {
        if (Objects.isNull(attachments)){
            attachments = new HashMap<>();
        }
        if (!attachments.containsKey(key)){
            attachments.put(key, value);
        }
    }
    @Override
    public String getMethodName() {
        return null;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return new Class[0];
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = Objects.isNull(parameterTypes) ? new Class<?>[0] : parameterTypes;
    }

    @Override
    public Object[] getArguments() {
        return new Object[0];
    }

    public void setArguments(Object[] arguments) {
        this.arguments = Objects.isNull(arguments) ? new Object[0] : arguments;
    }

    public void addAttachments(Map<String, String> attachments){
        if (Objects.isNull(attachments)){
            return;
        }
        if (Objects.isNull(this.attachments)){
            this.attachments = new HashMap<>();
        }
        this.attachments.putAll(attachments);
    }

    /**
     * 添加attachments集合防止重复
     * @param attachments
     */
    public void addAttachmentsIfAbsent(Map<String, String> attachments){
        if (Objects.isNull(attachments)){
            return;
        }
        if (Objects.isNull(this.attachments)){
            this.attachments = new HashMap<>();
        }
        for (Map.Entry<String, String> entry : attachments.entrySet()){
            setAttachmentIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public String getAttachment(String key) {
        if (Objects.isNull(key)){
            return null;
        }
        return attachments.get(key);
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        if (Objects.isNull(key)){
            return defaultValue;
        }
        String val = attachments.get(key);
        if (StringUtils.isBlank(val)){
            return defaultValue;
        }
        return val;
    }

    @Override
    public Invoker<?> getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    @Override
    public String toString() {
        return "RpcInvocatio [methodName=" + methodName + ", paramterTypes=" + Arrays.toString(parameterTypes) + ", arguments=" + Arrays.toString(arguments) + ", attachments=" + attachments + "]";
    }
}
