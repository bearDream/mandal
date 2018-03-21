package cn.ching.mandal.config;

import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.rpc.Protocol;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ServiceConfig extends AbstractServiceConfig{

    private static final long serialVersionUID = -3356538077473533827L;

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
}
