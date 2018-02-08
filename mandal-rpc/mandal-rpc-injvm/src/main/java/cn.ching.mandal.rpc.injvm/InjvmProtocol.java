package cn.ching.mandal.rpc.injvm;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.UrlUtils;
import cn.ching.mandal.rpc.Exporter;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.protocol.AbstractProtocol;
import cn.ching.mandal.rpc.support.ProtocolUtils;

import java.util.Map;
import java.util.Objects;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class InjvmProtocol extends AbstractProtocol implements Protocol {

    public static final String NAME = Constants.LOCAL_PROTOCOL;

    public static final int DEFAULT_PORT = 0;
    private static InjvmProtocol INSTANCE;

    public InjvmProtocol(){
        INSTANCE = this;
    }

    public static InjvmProtocol getINSTANCE() {
        if (Objects.isNull(INSTANCE)){
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(InjvmProtocol.NAME);
        }
        return INSTANCE;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    static Exporter<?> getExporter(Map<String, Exporter<?>> map, URL key){
        Exporter<?> result = null;

        if (!key.getServiceKey().contains("*")){
            result = map.get(key.getServiceKey());
        }else {
            if (!Objects.isNull(map) && !map.isEmpty()){
                for (Exporter<?> exporter : map.values()) {
                    if (UrlUtils.isServiceKeyMatch(key, exporter.getInvoker().getUrl())){
                        result = exporter;
                        break;
                    }
                }
            }
        }

        if (Objects.isNull(result)){
            return null;
        }else if (ProtocolUtils.isGeneric(result.getInvoker().getUrl().getParameter(Constants.GENERIC_KEY))){
            return null;
        }else {
            return result;
        }
    }


    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return new InjvmExporter<T>(invoker, invoker.getUrl().getServiceKey(), exporterMap);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        return new InjvmInvoker<>(type, url, url.getServiceKey(), exporterMap);
    }

    public boolean isInjvmRefer(URL url){
        final boolean isJvmRefer;
        String scope = url.getParameter(Constants.SCOPE_KEY);
        if (Constants.LOCAL_PROTOCOL.toString().equals(scope)){
            isJvmRefer = false;
        }else if (Constants.SCOPE_LOCAL.equals(scope) || (url.getParameter("injvm", false))){
            isJvmRefer = true;
        }else if (Constants.SCOPE_REMOTE.equals(scope)){
            isJvmRefer = false;
        }else if (url.getParameter(Constants.GENERIC_KEY, false)){
            isJvmRefer = false;
        }else if (getExporter(exporterMap, url) != null){
            isJvmRefer = true;
        }else {
            isJvmRefer = false;
        }
        return isJvmRefer;
    }
}
