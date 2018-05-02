package cn.ching.mandal.remoting.zookeeper;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/26
 * zkClient & curator
 * two way of zookeeper client.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("curator")
public interface ZookeeperTransporter {

    @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    ZookeeperClient connect(URL url);
}
