package cn.ching.mandal.rpc.cluster.merger;

import cn.ching.mandal.rpc.cluster.Merger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2018/1/17
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ListMerger implements Merger<List<?>> {

    @Override
    public List<?> merge(List<?>... items) {
        List res = new ArrayList();
        for (List<?> item : items) {
            if (!Objects.isNull(item)){
                res.addAll(item);
            }
        }
        return res;
    }
}
