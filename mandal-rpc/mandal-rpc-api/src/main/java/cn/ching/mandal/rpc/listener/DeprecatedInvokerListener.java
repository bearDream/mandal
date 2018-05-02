package cn.ching.mandal.rpc.listener;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;

/**
 * 2018/3/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate(Constants.DEPRECATED_KEY)
public class DeprecatedInvokerListener extends InvokerListenerAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedInvokerListener.class);

    @Override
    public void referred(Invoker<?> invoker) throws RpcException {
        if (invoker.getUrl().getParameter(Constants.DEPRECATED_KEY, false)){
            logger.warn("The service " + invoker.getInterface().getName() + " is DEPRECATED. Declare from: " + invoker.getUrl());
        }
    }
}
