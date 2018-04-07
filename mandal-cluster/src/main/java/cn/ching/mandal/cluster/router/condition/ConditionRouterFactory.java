package cn.ching.mandal.cluster.router.condition;

import cn.ching.mandal.cluster.Router;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.cluster.RouterFactory;

/**
 * 2018/1/17
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConditionRouterFactory implements RouterFactory {

    public static final String NAME = "condition";

    @Override
    public Router getRouter(URL url) {
        return new ConditionRouter(url);
    }
}
