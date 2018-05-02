package cn.ching.mandal.common.status;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/3/8
 * StatusChecker
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface StatusChecker {

    /**
     * check status
     * @return status
     */
    Status check();
}
