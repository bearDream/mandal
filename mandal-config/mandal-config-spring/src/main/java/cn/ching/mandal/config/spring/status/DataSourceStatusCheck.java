package cn.ching.mandal.config.spring.status;

import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.status.Status;
import cn.ching.mandal.common.status.StatusChecker;

/**
 * 2018/3/25
 * todo
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate
public class DataSourceStatusCheck implements StatusChecker{
    @Override
    public Status check() {
        return null;
    }
}
