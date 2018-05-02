package cn.ching.mandal.rpc.filter;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.rpc.*;

import java.util.Map;
import java.util.Objects;

/**
 * 2018/1/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, value = Constants.TOKEN_KEY)
public class TokenFilter implements Filter {


    @Override
    public Result invoker(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String token = invoker.getUrl().getParameter(Constants.TOKEN_KEY);
        if (ConfigUtils.isNotEmpty(token)){
            Class<?> serviceType = invoker.getInterface();
            Map<String, String> attachments = invocation.getAttachments();
            String remoteToken = Objects.isNull(attachments) ? null : attachments.get(Constants.TOKEN_KEY);
            if (!token.equals(remoteToken)){
                throw new RpcException("Invalid token! can's invoke remote service:" + serviceType + " method:" + invocation.getMethodName() +" from consumer: "+ RpcContext.getContext().getRemoteHost() + " to provider: " + RpcContext.getContext().getLocalHost());
            }
        }
        return invoker.invoke(invocation);
    }
}
