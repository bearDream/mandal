package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Result;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.LoadBalance;
import cn.ching.mandal.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractClusterInvoker<T> implements Invoker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterInvoker.class);

    protected final Directory<T> directory;

    /**
     * if url parameter's availableCheck is true, then check service available, otherwise don't check it.
     */
    protected final boolean availableCheck;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    private volatile Invoker<T> stickyInvoker = null;

    public AbstractClusterInvoker(Directory<T> directory){
        this(directory, directory.getUrl());
    }

    public AbstractClusterInvoker(Directory<T> directory, URL url){
        if (Objects.isNull(directory)){
            throw new IllegalArgumentException("service directory is null");
        }

        this.directory = directory;
        this.availableCheck = url.getParameter(Constants.CLUSTER_AVAILABLE_CHECK_KEY, Constants.DEFAULT_CLUSTER_AVAILABLE_CHECK);
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public URL getUrl() {
        return directory.getUrl();
    }

    @Override
    public boolean isAvailable() {
        Invoker<T> invoker = stickyInvoker;
        if (!Objects.isNull(invoker)){
            return invoker.isAvailable();
        }
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)){
            directory.destroy();
        }
    }

    @Override
    public Result invoke(Invocation invocation) {

        checkWhetherDestryed();

        LoadBalance loadBalance;
        List<Invoker<T>> invokers = list(invocation);
        if (CollectionUtils.isNotEmpty(invokers)){
            loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl().getMethodParameter(invocation.getMethodName(), Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE));
        }else {
            loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE);
        }
        RpcUtils.attachInvokeIdIfAsync(getUrl(), invocation);
        return doInvoker(invocation, invokers, loadBalance);
    }

    protected abstract Result doInvoker(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadBalance) throws RpcException;

    /**
     * check directory is destryed
     */
    protected void checkWhetherDestryed(){
        if (destroyed.get()){
            throw new RpcException("Rpc cluster invoker for " + getInterface() + " on consumer " + NetUtils.getLocalHost()
                        + "use mandal version is " + Version.getVersion()
                        + "is destryed, can't invoker anymore");
        }
    }

    protected List<Invoker<T>> list(Invocation invocation) throws RpcException{
        List<Invoker<T>> invokers = directory.list(invocation);
        return invokers;
    }

    @Override
    public String toString() {
        return getInterface() + " ->" + getUrl().toString();
    }

    /**
     * Select a invoker using loadbalance policy.
     * 1、select an invoker using loadbalance. if the invoker is in previously list or is unavailable, then continue reselect. otherwise return the first selected invokers.
     * 2、Reselection. the validation rules: selected > available,
     * @param loadBalance  loadBalance policy
     * @param invocation
     * @param invokers  invoker candidates
     * @param selected  exclude selected invokers or not
     * @return
     * @throws RpcException
     */
    protected Invoker<T> select(LoadBalance loadBalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException{
        if (CollectionUtils.isEmpty(invokers)){
            return  null;
        }
        String methodName = Objects.isNull(invocation) ? "" : invocation.getMethodName();

        boolean sticky = invokers.get(0).getUrl().getMethodParameter(methodName, Constants.CLUSTER_STICKY_KEY, Constants.DEFAULT_CLUSTER_STICKY);

        if (Objects.nonNull(sticky) && !invokers.contains(stickyInvoker)){
            stickyInvoker = null;
        }
        if (sticky && Objects.nonNull(stickyInvoker) && (selected == null || !selected.contains(stickyInvoker))){
            if (availableCheck && stickyInvoker.isAvailable()){
                return stickyInvoker;
            }
        }

        Invoker invoker = doSelect(loadBalance, invocation, invokers, selected);

        if (sticky){
            stickyInvoker = invoker;
        }
        return invoker;
    }

    /**
     * select a invoker.
     * 1、if invokers only have two invoker, then use round-robin instead
     * 2、otherwise use loadbalance policy.
     * 3、if the invoker not Available, then reselect
     * @param loadBalance
     * @param invocation
     * @param invokers
     * @param selected
     * @return
     */
    private Invoker<T> doSelect(LoadBalance loadBalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) {
        if (invokers.size() == 1){
            return  invokers.get(0);
        }
        // if we only have two invokers, then use round-robin instead
        if (invokers.size() == 2 && selected != null && selected.size() > 0){
            return selected.get(0) == invokers.get(0) ? invokers.get(1) : invokers.get(0);
        }
        // use loadbalance policy
        Invoker invoker = loadBalance.select(invokers, getUrl(), invocation);

        if (Objects.nonNull(selected) && selected.contains(invoker) || (!invoker.isAvailable() && getUrl() != null && availableCheck)){
            try {
                Invoker<T> rinvoker = reselect(loadBalance, invocation, invokers, selected, availableCheck);
                if (!Objects.isNull(rinvoker)){
                    invoker = rinvoker;
                }else {
                    int index = invokers.indexOf(invoker);
                    try {
                        invoker = index < invokers.size() - 1 ? invokers.get(index+1) : invoker;
                    }catch (Exception e){
                        LOGGER.warn(e.getMessage() + " may because invokers list dynamic change, ignore ", e);
                    }
                }
            }catch (Throwable t){
                LOGGER.error("cluster reselect fail reason is : " + t.getMessage() + " if can't solve it, you can set cluster.availablecheck=false in url" + t);
            }
        }
        return invoker;
    }

    /**
     * reselect.
     * 1、select a invoker not in 'selected'
     * 2、all invokers is selected, then using loadbalance policy select one invoker.
     * @param loadBalance
     * @param invocation
     * @param invokers
     * @param selected
     * @param availableCheck
     * @return
     */
    private Invoker<T> reselect(LoadBalance loadBalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected, boolean availableCheck) throws RpcException {

        List<Invoker<T>> reselectInvokers = new ArrayList<>(invokers.size() > 1 ? invokers.size()-1 : invokers.size());
        // select a invoker not in 'selected'
        if (availableCheck){
            // check available
            invokers.stream().filter(i -> i.isAvailable() == true).filter(i -> selected == null && selected.contains(i)).forEach(i -> {
                reselectInvokers.add(i);
            });
        }else {
            // not check available
            invokers.stream().filter(i -> selected == null && selected.contains(i)).forEach(i -> {
                reselectInvokers.add(i);
            });
            if (reselectInvokers.size() > 0){
                return loadBalance.select(invokers, getUrl(), invocation);
            }
        }
        // all invokers is selected, then using loadbalance policy select one invoker.
        if (Objects.nonNull(selected)){
            invokers.stream().filter(i -> i.isAvailable()==true && !reselectInvokers.contains(i)).forEach(i -> {
                reselectInvokers.add(i);
            });
        }
        if (reselectInvokers.size() > 0){
            return loadBalance.select(reselectInvokers, getUrl(), invocation);
        }

        return null;
    }

    protected void checkInvokers(List<Invoker<T>> invokers, Invocation invocation){
        if (CollectionUtils.isEmpty(invokers)){
            throw new RpcException("failed invoke the method" + invocation.getMethodName() + " in the service " + getInterface().getName()
                        + ". No provider available for the service " + directory.getUrl().getServiceKey()
                        + " from registry" + directory.getUrl().getAddress()
                        + " on the consumer " + NetUtils.getLocalHost()
                        + "using the mandal version is " + Version.getVersion()
                        + ". checked if the providers have been started and registried");
        }
    }


}
