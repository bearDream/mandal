package cn.ching.mandal.rpc.protocol;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.support.RpcUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2018/1/12
 * Invoker template
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Class<T> mType;

    private final URL mUrl;

    private final Map<String, String> mAttachments;

    private volatile boolean available = true;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractInvoker(Class<T> type, URL url){
        this(type, url, (Map<String, String>) null);
    }

    public AbstractInvoker(Class<T> type, URL url, String[] keys){
        this(type, url, convertAttachments(url, keys));
    }

    public AbstractInvoker(Class<T> type, URL url, Map<String, String> attachments){
        if (Objects.isNull(type)){
            throw new IllegalArgumentException("service type is null");
        }
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("service url is null");
        }
        this.mType = type;
        this.mUrl = url;
        this.mAttachments = Objects.isNull(attachments) ? null : Collections.unmodifiableMap(attachments);
    }

    private static Map<String,String> convertAttachments(URL url, String[] keys) {
        if (Objects.isNull(keys) || keys.length == 0){
            return null;
        }
        Map<String, String> attachments = new HashMap<>();
        for (String s : keys){
            String val = url.getParameter(s);
            if (!StringUtils.isBlank(val)){
                attachments.put(s, val);
            }
        }
        return attachments;
    }

    protected void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isDestroyed(){
        return destroyed.get();
    }

    @Override
    public String toString() {
        return getInterface() + "->" + (Objects.isNull(getUrl()) ? "" : getUrl().toString());
    }

    @Override
    public Class<T> getInterface() {
        return mType;
    }

    @Override
    public URL getUrl() {
        return mUrl;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        if(destroyed.compareAndSet(false, true)){
            return;
        }
        setAvailable(false);
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException{
        if (destroyed.get()) {
            throw new RpcException("Rpc invoker for service " + this + "on consumer " + NetUtils.getLocalHost()
                    + "use mandal version is " + Version.getVersion()
                    + "is destroyed, can not be invoked any more");
        }
        RpcInvocation rpcInvocation = (RpcInvocation) invocation;
        rpcInvocation.setInvoker(this);
        if (Objects.nonNull(mAttachments) && mAttachments.size() > 0){
            rpcInvocation.addAttachments(mAttachments);
        }
        Map<String, String> context = RpcContext.getContext().getAttachments();
        if (Objects.nonNull(context)){
            ((RpcInvocation) invocation).addAttachmentsIfAbsent(context);
        }
        if (getUrl().getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY, false)){
            ((RpcInvocation) invocation).setAttachment(Constants.ASYNC_KEY, Boolean.TRUE.toString());
        }

        RpcUtils.attachInvokeIdIfAsync(getUrl(), invocation);

        try {
            return doInvoke(invocation);
        }catch (InvocationTargetException te){
            Throwable t = te.getTargetException();
            if (Objects.isNull(t)){
                return new RpcResult(t);
            }else {
                if (t instanceof RpcException){
                    ((RpcException) t).setCode(RpcException.BIZ_EXCEPTION);
                }
                return new RpcResult(t);
            }

        }catch (RpcException re){
            if (re.isBiz()){
                return new RpcResult(re);
            }else {
                throw re;
            }
        }catch (Throwable t){
            return new RpcResult(t);
        }
    }

    protected abstract Result doInvoke(Invocation invocation) throws Throwable;
}
