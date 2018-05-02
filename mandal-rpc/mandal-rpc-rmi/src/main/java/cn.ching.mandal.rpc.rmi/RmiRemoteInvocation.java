package cn.ching.mandal.rpc.rmi;

import cn.ching.mandal.rpc.RpcContext;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * 2018/1/29
 *
 * Need to restore context on provider side (Though context will be overridden by Invocation's attachment
 * when ContextFilter gets executed, we will restore the attachment when Invocation is constructed.
 * @see cn.ching.mandal.rpc.proxy.InvokerInvocationHandler
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RmiRemoteInvocation extends RemoteInvocation {

    private static final long serialVersionUID = 6946936222491439228L;

    private static final String mandalAttachmentsAttrName = "mandal.attachments";

    public RmiRemoteInvocation(MethodInvocation invocation){
        super(invocation);
        addAttribute(mandalAttachmentsAttrName, new HashMap<String, String>(RpcContext.getContext().getAttachments()));
    }

    @Override
    public Object invoke(Object targetObject) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        RpcContext context = RpcContext.getContext();
        context.setAttachments((Map<String, String>) getAttribute(mandalAttachmentsAttrName));
        try {
            return super.invoke(targetObject);
        }finally {
            context.setAttachments(null);
        }
    }
}
