package cn.ching.mandal.regitry.api;

import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.registry.RegistryFactory;
import cn.ching.mandal.rpc.Protocol;
import org.junit.Test;

/**
 * 2018/4/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ExtensionLoaderTest {

    @Test
    public void TestRegistryFactory(){
//        boolean registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension("mandal");
        boolean rmi = ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension("rmi");
//        System.out.println(registryFactory);
    }
}
