package cn.ching.mandal.rpc.rmi;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.protocol.AbstractProxyProtocol;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.Objects;

/**
 * 2018/1/29
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RmiProtocol extends AbstractProxyProtocol{

    public static final int DEFAULT_PORT = 1099;

    public RmiProtocol(){
        super(RemoteAccessException.class, RemoteException.class);
    }

    @Override
    protected <T> Runnable doExport(T proxy, Class<T> type, URL url) throws RpcException {
        final RmiServiceExporter rmiServiceExporter = new RmiServiceExporter();
        rmiServiceExporter.setRegistryPort(url.getPort());
        rmiServiceExporter.setServiceName(url.getPath());
        rmiServiceExporter.setServiceInterface(type);
        rmiServiceExporter.setService(proxy);
        try {
            rmiServiceExporter.afterPropertiesSet();
        }catch (RemoteException e){
            throw new RpcException(e.getMessage(), e);
        }
        return () -> {
            try {
                rmiServiceExporter.destroy();
            }catch (Throwable t){
                logger.warn(t.getMessage(), t);
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        final RmiProxyFactoryBean rmiProxyFactoryBean = new RmiProxyFactoryBean();
        rmiProxyFactoryBean.setRemoteInvocationFactory((methodInvocation -> new RmiRemoteInvocation(methodInvocation)));
        rmiProxyFactoryBean.setServiceUrl(url.toIdentityString());
        rmiProxyFactoryBean.setServiceInterface(type);
        rmiProxyFactoryBean.setCacheStub(false);
        rmiProxyFactoryBean.setLookupStubOnStartup(true);
        rmiProxyFactoryBean.setRefreshStubOnConnectFailure(true);
        rmiProxyFactoryBean.afterPropertiesSet();
        return (T) rmiProxyFactoryBean.getObject();
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected int getErrorCode(Throwable t){
        if (t instanceof RemoteAccessException){
            t = t.getCause();
        }
        if (!Objects.isNull(t) && !Objects.isNull(t.getCause())){
            Class<?> clazz = t.getCause().getClass();
            if (SocketTimeoutException.class.equals(clazz)){
                return RpcException.TIMEOUT_EXCEPTION;
            }else if (IOException.class.equals(clazz)){
                return RpcException.NETWORK_EXCEPTION;
            }else if (ClassNotFoundException.class.equals(clazz)){
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(t);
    }
}
