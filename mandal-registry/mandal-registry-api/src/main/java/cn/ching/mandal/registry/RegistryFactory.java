package cn.ching.mandal.registry;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/15
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("mandal")
public interface RegistryFactory {

    /**
     * Connect to the registry
     * <p>
     * Connecting the registry needs to support the contract: <br>
     * 1. When the check=false is set, the connection is not checked, otherwise the exception is thrown when disconnection <br>
     * 2. Support username:password authority authentication on URL.<br>
     * 3. Support the backup=10.20.153.10 candidate registry cluster address.<br>
     * 4. Support file=registry.cache local disk file cache.<br>
     * 5. Support the timeout=1000 request timeout setting.<br>
     * 6. Support session=60000 session timeout or expiration settings.<br>
     *
     * @param url Registry address, is not allowed to be empty
     * @return Registry reference, never return empty value
     */
    @Adaptive({"protocol"})
    Registry getRegistry(URL url);
}
