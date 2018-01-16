package cn.ching.mandal.rpc.cluster;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface Merger<T> {

    T merge(T... items);
}
