package cn.ching.mandal.rpc.cluster.directory;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.Router;
import cn.ching.mandal.rpc.cluster.RouterFactory;
import cn.ching.mandal.rpc.cluster.router.MockInvokerSelector;

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
public abstract class AbstractDirectory implements Directory{

    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false;

    private volatile URL consumerUrl;

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
    public Class getInterface() {
        return null;
    }

    @Override
    public List<Invoker> list(Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

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
}
