package cn.ching.mandal.rpc.cluster.directory;

import cn.ching.mandal.rpc.cluster.Router;
import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.RouterFactory;
import cn.ching.mandal.rpc.cluster.router.MockInvokerSelector;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false;

    @Getter
    @Setter
    private volatile URL consumerUrl;

    @Getter
    private volatile List<Router> routers;

    public AbstractDirectory(URL url){
        this(url, null);
    }

    public AbstractDirectory(URL url, List<Router> routers){
        this(url, url, routers);
    }

    public AbstractDirectory(URL url, URL consumerUrl, List<Router> routers){
        if (Objects.isNull(url)){
            throw new IllegalArgumentException("url is null");
        }
        this.url = url;
        this.consumerUrl = consumerUrl;
        this.routers = routers;
    }

    @Override
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed){
            throw new RpcException("directory was destroyed! url: " + getUrl());
        }
        List<Invoker<T>> invokers = doList(invocation);
        List<Router> localRouters = this.routers;
        if (CollectionUtils.isNotEmpty(localRouters)){
            for (Router localRouter : localRouters) {
                try {
                    if (Objects.nonNull(getUrl()) || localRouter.getUrl().getParameter(Constants.RUNTIME_KEY, false)){
                        invokers = localRouter.route(invokers, getConsumerUrl(), invocation);
                    }
                }catch (Throwable t){
                    logger.error("failed execute router " + getUrl() + ", cause: " + t.getMessage(), t);
                }
            }
        }
        return invokers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setRouters(List<Router> routers) {
        routers = Objects.isNull(routers) ? new ArrayList<>() : new ArrayList<>(routers);
        String routingKey = url.getParameter(Constants.ROUTER_KEY);
        if (!StringUtils.isBlank(routingKey)){
            RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(routingKey);
            routers.add(routerFactory.getRouter(url));
        }

        routers.add(new MockInvokerSelector());
        Collections.sort(routers);
        this.routers = routers;
    }

    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException;
}
