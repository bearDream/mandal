package cn.ching.mandal.config.spring.status;

import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.status.Status;
import cn.ching.mandal.common.status.StatusChecker;
import org.springframework.context.ApplicationContext;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate
public class SpringStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(SpringStatusChecker.class);

    @Override
    public Status check() {
//        ApplicationContext context = servicebean
        return null;
    }
}
