package cn.ching.mandal.common;

import cn.ching.mandal.common.URL;

/**
 * 2018/1/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Node {

    URL getUrl();

    boolean isAvailable();

    void destroy();
}
