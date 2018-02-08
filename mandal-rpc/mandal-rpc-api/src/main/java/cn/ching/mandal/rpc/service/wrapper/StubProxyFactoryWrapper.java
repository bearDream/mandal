package cn.ching.mandal.rpc.service.wrapper;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.bytecode.Wrapper;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.common.utils.ReflectUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.service.GenericService;
import lombok.Setter;

import java.lang.reflect.Constructor;

/**
 * 2018/1/15
 * decorate proxy factory
 * add url parameter mandal.stub.event.methods and isserver
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class StubProxyFactoryWrapper implements ProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StubProxyFactoryWrapper.class);

    private final ProxyFactory proxyFactory;

    @Setter
    private Protocol protocol;

    public StubProxyFactoryWrapper(ProxyFactory proxyFactory){
        this.proxyFactory = proxyFactory;
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        T proxy = proxyFactory.getProxy(invoker);
        if (GenericService.class != invoker.getInterface()){
            String stub = invoker.getUrl().getParameter(Constants.STUB_KEY, invoker.getUrl().getParameter(Constants.LOCAL_KEY));
            if (ConfigUtils.isNotEmpty(stub)){
                Class<?> serviceType = invoker.getInterface();
                if (ConfigUtils.isDefault(stub)){
                    if (invoker.getUrl().hasParameter(Constants.STUB_KEY)){
                        stub = serviceType.getName() + "Stub";
                    }else {
                        stub = serviceType.getName() + "Local";
                    }
                }
                try {
                    Class<?> stubClass = ReflectUtils.forName(stub);
                    if (!serviceType.isAssignableFrom(stubClass)){
                        throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interfaces " + serviceType.getName());
                    }
                    try {
                        Constructor<?> constructor = ReflectUtils.findConstructor(stubClass, serviceType);
                        proxy = (T) constructor.newInstance(new Object[]{proxy});

                        URL url = invoker.getUrl();
                        if (url.getParameter(Constants.STUB_EVENT_KEY, Constants.DEFAULT_STUB_EVENT)){
                            url.addParameter(Constants.STUB_EVENT_METHODS_KEY, StringUtils.join(Wrapper.getWrapper(proxy.getClass()).getDeclaredMethodNames(), ","));
                            url.addParameter(Constants.IS_SERVER_KEY, Boolean.FALSE.toString());
                            try {
                                exporter(proxy, invoker.getInterface(), url);
                            }catch (Exception e){
                                LOGGER.error("export stub service error: cause by:", e);
                            }
                        }
                    }catch (NoSuchMethodException me){
                        throw new IllegalStateException("no such constrcutors \" public " + stubClass.getSimpleName() + "(" + serviceType.getName() + ") \" in stub implements", me);
                    }
                }catch (Throwable t){
                    LOGGER.error("failed create stub implementation class " + stub + "in consumer" + NetUtils.getLocalHost() + " use mandal version is: " + Version.getVersion() + " cause by: " + t.getMessage(), t);
                }
            }
        }
        return proxy;
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(proxy, type, url);
    }

    private <T> Exporter<T> exporter(T instance, Class<T> type, URL url){
        return protocol.export(proxyFactory.getInvoker(instance, type, url));
    }
}
