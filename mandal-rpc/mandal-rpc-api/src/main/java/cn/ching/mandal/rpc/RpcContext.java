package cn.ching.mandal.rpc;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.common.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

/**
 * 2018/1/11
 * threadSafe threadLocal
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RpcContext {

    private static final ThreadLocal<RpcContext> LOCAL = ThreadLocal.withInitial(RpcContext::new);

    @Getter
    private final Map<String, String> attachments = new HashMap<>();

    private final Map<String, Object> values = new HashMap<>();

    private Future<?> future;

    private List<URL> urls;

    @Getter
    @Setter
    private URL url;

    @Setter
    @Getter
    private String methodName;

    @Getter
    @Setter
    private Class<?>[] parameterTypes;

    @Getter
    @Setter
    private Object[] arguments;

    private InetSocketAddress localAddress;

    @Getter
    private InetSocketAddress remoteAddress;

    protected RpcContext(){

    }

    public static RpcContext getContext(){
        return LOCAL.get();
    }

    public static void remove(){
        LOCAL.remove();
    }

    public boolean isProvider(){
        URL url = getUrl();
        if (Objects.isNull(url)){
            return false;
        }
        InetSocketAddress address = getRemoteAddress();
        if (Objects.isNull(address)){
            return false;
        }
        String host;
        if (Objects.isNull(address.getAddress())){
            host = address.getHostName();
        }else {
            host = address.getAddress().getHostAddress();
        }

        return url.getPort() != address.getPort()
                || !NetUtils.filterLocalHost(url.getIp()).equals(NetUtils.filterLocalHost(host));
    }

    public boolean isConsumer(){
        URL url = getUrl();
        if (Objects.isNull(url)){
            return false;
        }
        InetSocketAddress address = getRemoteAddress();
        if (Objects.isNull(address)){
            return false;
        }
        String host;
        if (Objects.isNull(address.getAddress())){
            host = address.getHostName();
        }else {
            host = address.getAddress().getHostAddress();
        }

        return url.getPort() == address.getPort()
                && NetUtils.filterLocalHost(url.getIp()).equals(NetUtils.filterLocalHost(host));
    }

    public <T> Future<T> getFuture() {
        return (Future<T>) future;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }

    public List<URL> getUrls() {
        return urls == null && url != null ? Arrays.asList(url) : urls;
    }

    public void setUrls(List<URL> urls) {
        this.urls = urls;
    }

    public RpcContext setLocalAddress(String host, int port) {
        if (port < 0){
            port = 0;
        }
        this.localAddress = InetSocketAddress.createUnresolved(host, port);
        return this;
    }

    public String getLocalAddressString() {
        return getLocalHost() + ":" + getLocalPort();
    }

    public String getLocalHostName(){
        String host = localAddress == null ? null : localAddress.getHostName();
        if (host == null || host.length() == 0){
            return getLocalHost();
        }
        return host;
    }

    public RpcContext setRemoteAddress(String host, int port) {
        if (port < 0){
            port = 0;
        }
        this.remoteAddress = InetSocketAddress.createUnresolved(host, port);
        return this;
    }

    public RpcContext setRemoteAddress(InetSocketAddress address) {
        this.remoteAddress = address;
        return this;
    }

    public String getRemoteAddressString() {
        return getRemoteAddress() + ":" + getRemotePort();
    }

    public String getRemoteHostName(){
        return Objects.isNull(remoteAddress) ? null : remoteAddress.getHostName();
    }

    public int getRemotePort() {
        return Objects.isNull(remoteAddress) ? 0 : remoteAddress.getPort();
    }

    public int getLocalPort() {
        return localAddress == null ? 0 : localAddress.getPort();
    }

    public String getLocalHost() {
        String host = localAddress == null ? null :
                localAddress.getAddress() == null ? localAddress.getHostName()
                        : NetUtils.filterLocalHost(localAddress.getAddress().getHostAddress());
        if (StringUtils.isBlank(host)){
            return NetUtils.getLocalHost();
        }
        return host;
    }

    public String getRemoteHost(){
        return Objects.isNull(remoteAddress) ? null :
                Objects.isNull(remoteAddress.getAddress()) ? remoteAddress.getHostName() : NetUtils.filterLocalHost(remoteAddress.getAddress().getHostAddress());
    }

    public String getAttachment(String key){
        return attachments.get(key);
    }

    public RpcContext setAttachment(String key, String value){
        if (Objects.isNull(value)){
            attachments.remove(key);
        }else {
            attachments.put(key, value);
        }
        return this;
    }

    public RpcContext removeAttachment(String key){
        attachments.remove(key);
        return this;
    }

    public RpcContext setAttachment(Map<String, String> attachment){
        this.attachments.clear();
        if (Objects.nonNull(attachment) && attachment.size() > 0){
            this.attachments.putAll(attachment);
        }
        return this;
    }

    public RpcContext setAttachments(Map<String, String> attachments){
        this.attachments.clear();
        if (!Objects.isNull(attachments) && attachments.size() > 0){
            this.attachments.putAll(attachments);
        }
        return this;
    }

    public void clearAttachments(){
        this.attachments.clear();
    }

    public Map<String, Object> get(){
        return values;
    }

    public RpcContext set(String key, Object value){
        if (Objects.isNull(value)){
            values.remove(key);
        }else {
            values.put(key, value);
        }
        return this;
    }

    public RpcContext remove(String key){
        values.remove(key);
        return this;
    }

    public Object get(String key){
        return values.get(key);
    }

    public RpcContext setInvokers(List<Invoker> invokers){
        if (invokers != null && invokers.size() > 0){
            List<URL> urls = new ArrayList<URL>(invokers.size());
            invokers.forEach(i -> urls.add(i.getUrl()));
            setUrls(urls);
        }
        return this;
    }

    public RpcContext setInvokers(Invoker invoker){
        if (Objects.nonNull(invoker)){
            setUrl(invoker.getUrl());
        }
        return this;
    }

    public RpcContext setInvocation(Invocation invocation){
        if (Objects.nonNull(invocation)){
            setMethodName(invocation.getMethodName());
            setParameterTypes(invocation.getParameterTypes());
            setArguments(invocation.getArguments());
        }
        return this;
    }

    public <T> Future<T> asycCall(Callable<T> callable){
        try {
            try {
                setAttachment(Constants.ASYNC_KEY, Boolean.TRUE.toString());
                final T o = callable.call();
                if (Objects.nonNull(o)){
                    FutureTask task = new FutureTask<T>(() -> {return o;});
                    task.run();
                    return task;
                }
            } catch (Exception e) {
                throw new RpcException();
            }finally {
                removeAttachment(Constants.ASYNC_KEY);
            }
        } catch (RpcException e){
            return new Future<T>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public T get() throws InterruptedException, ExecutionException {
                    throw new ExecutionException(e.getCause());
                }

                @Override
                public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return get();
                }
            };
        }
        return getContext().getFuture();
    }

    public void asycCall(Runnable runnable){
        try {
            setAttachment(Constants.RETURN_KEY, Boolean.FALSE.toString());
            runnable.run();
        }catch (Throwable t){
            throw new RpcException("oneway call error ." + t.getMessage(), t);
        } finally {
            removeAttachment(Constants.RETURN_KEY);
        }
    }
}
