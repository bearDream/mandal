package cn.ching.mandal.rpc.cluster;

import cn.ching.mandal.rpc.cluster.merger.ListMerger;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/16
 * merger can have many operation.
 * such as list, array, map...
 * For the time being there is only one realization
 * @see ListMerger
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface Merger<T> {

    T merge(T... items);
}
