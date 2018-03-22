package cn.ching.mandal.config;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.ProxyFactory;
import cn.ching.mandal.rpc.cluster.Cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ReferenceConfig<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = 6846610815421107297L;

    private static final Protocol reprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private final List<URL> urls = new ArrayList<>();

    private String interfaceName;
    private Class<?> interfaceClass;

    private String client;
    // peer-to-peer invocation.
    private String url;

    // method configs
    private List<MethodConfig> methods;
    // default config
    private ConsumerConfig consumer;
    private String protocol;
    // interface proxy reference
    private transient volatile T ref;
    private transient volatile Invoker<T> invoker;
    private transient volatile boolean initialized;
    private transient volatile boolean destroyed;


    private final Object finalizeGuadian = new Object(){
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            if (!ReferenceConfig.this.destroyed){
                logger.warn("ReferenceConfig (" + url + ") is not destroyed when FINALIZE");
            }
        }
    };

    public ReferenceConfig(){

    }

    public ReferenceConfig(Reference reference){
        appendAnnotation(Reference.class, reference);
    }








}
