package cn.ching.mandal.common.filter.support;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.filter.Filter;

/**
 * 2018/1/10
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ExFilter implements Filter {

    @Override
    public void invoke(URL url) {
        System.out.println("this is ExFilter");
    }
}
