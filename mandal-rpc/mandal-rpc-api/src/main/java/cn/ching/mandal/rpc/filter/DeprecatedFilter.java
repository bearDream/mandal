package cn.ching.mandal.rpc.filter;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.rpc.*;

import java.util.Set;

@Activate(group = Constants.CONSUMER, value = Constants.DEPRECATED_KEY)
public class DeprecatedFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeprecatedFilter.class);

    private static final Set<String> logged = new ConcurrentHashSet<String>();

    @Override
    public Result invoker(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
        if (!logged.contains(key)) {
            logged.add(key);
            if (invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.DEPRECATED_KEY, false)) {
                LOGGER.error("The service method " + invoker.getInterface().getName() + "." + getMethodSignature(invocation) + " is DEPRECATED! Declare from " + invoker.getUrl());
            }
        }
        return invoker.invoke(invocation);
    }

    private String getMethodSignature(Invocation invocation) {
        StringBuilder buf = new StringBuilder(invocation.getMethodName());
        buf.append("(");
        Class<?>[] types = invocation.getParameterTypes();
        if (types != null && types.length > 0) {
            boolean first = true;
            for (Class<?> type : types) {
                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(type.getSimpleName());
            }
        }
        buf.append(")");
        return buf.toString();
    }

}
