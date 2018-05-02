package cn.ching.mandal.config.spring.status;

import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.status.Status;
import cn.ching.mandal.common.status.StatusChecker;
import cn.ching.mandal.config.spring.ServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 2018/3/9
 * SpringStatusChecker
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate
public class SpringStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(SpringStatusChecker.class);

    @Override
    public Status check() {
        ApplicationContext context = ServiceBean.getSPRING_CONTEXT();
        if (Objects.isNull(context)){
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        if (context instanceof Lifecycle){
            if (((Lifecycle) context).isRunning()){
                level = Status.Level.OK;
            }else {
                level = Status.Level.ERROR;
            }
        }else {
            level = Status.Level.UNKNOWN;
        }
        StringBuffer sb = new StringBuffer();
        try {
            Class<?> cls = context.getClass();
            Method method = null;
            while (!Objects.isNull(cls) && Objects.isNull(method)){
                try {
                    method = cls.getDeclaredMethod("getConfigLocations",new Class<?>[0]);
                }catch (NoSuchMethodException e){
                    cls = cls.getSuperclass();
                }
            }
            if (!Objects.isNull(method)){
                if (!method.isAccessible()){
                    method.setAccessible(true);
                }
                String[] configs = (String[]) method.invoke(context, new Object[0]);
                if (!Objects.isNull(configs) && configs.length > 0){
                    for (String config : configs) {
                        if (config.length() > 0){
                            sb.append(",");
                        }
                        sb.append(config);
                    }
                }
            }
        }catch (Throwable t){
            logger.warn(t.getMessage(), t);
        }
        return new Status(level, sb.toString());
    }
}
