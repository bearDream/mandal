package cn.ching.mandal.rpc.listener;

import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.InvokerListener;
import cn.ching.mandal.rpc.RpcException;

/**
 * 2018/3/25
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class InvokerListenerAdaptor implements InvokerListener{

    @Override
    public void referred(Invoker<?> invoker) throws RpcException {

    }

    @Override
    public void destroyed(Invoker<?> invoker) throws RpcException {
        
    }
}
