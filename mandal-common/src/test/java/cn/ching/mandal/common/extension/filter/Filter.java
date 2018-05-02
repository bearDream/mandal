package cn.ching.mandal.common.extension.filter;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/10
 * 无任何作用，只为了测试生成Filter的adaptive适配类
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI
public interface Filter {

    @Adaptive
    void invoke(URL url);
}
