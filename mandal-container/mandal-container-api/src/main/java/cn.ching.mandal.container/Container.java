package cn.ching.mandal.container;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/30
 * Container. (SPI)
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("spring")
public interface Container {

    void start();

    void stop();
}
