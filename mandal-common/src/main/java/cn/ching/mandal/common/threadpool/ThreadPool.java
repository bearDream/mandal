package cn.ching.mandal.common.threadpool;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

import java.util.concurrent.Executor;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("fixed")
public interface ThreadPool {

    /**
     * thread pool
     * @param url
     * @return
     */
    @Adaptive({Constants.THREADPOOL_KEY})
    Executor getExecutor(URL url);
}
