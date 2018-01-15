package cn.ching.mandal.rpc.proxy;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.utils.ReflectUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.ProxyFactory;
import cn.ching.mandal.rpc.RpcException;
import cn.ching.mandal.rpc.service.GenericService;

import java.util.Objects;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractProxyFactory implements ProxyFactory{

    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        Class<?>[] interfaces = null;
        String config = invoker.getUrl().getParameter(Constants.INTERFACES);
        if (!StringUtils.isBlank(config)){
            String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
            if (Objects.nonNull(types) && types.length > 0){
                interfaces = new Class<?>[types.length+1];
                interfaces[0] = invoker.getInterface();
                for (int i = 0; i < types.length; i++){
                    interfaces[i+1] = ReflectUtils.forName(types[i]);
                }
            }
        }
        if (interfaces == null){
            interfaces = new Class<?>[]{invoker.getInterface(), GenericService.class};
        }
        return getProxy(invoker, interfaces);
    }

    public abstract  <T> T getProxy(Invoker<T> invoker, Class<?>[] types) throws RpcException;
}
