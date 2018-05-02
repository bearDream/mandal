package cn.ching.mandal.container.spring;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.container.Container;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Objects;

/**
 * 2018/1/30
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class SpringContainer implements Container {

    public static final String SPRING_CONFIG = "mandal.spring.config";
    public static final String DEFAULT_SPRING_CONFIG = "classpath*:META-INF/spring/*.xml";
    private static final Logger logger = LoggerFactory.getLogger(SpringContainer.class);
    private static ClassPathXmlApplicationContext context;

    public static ClassPathXmlApplicationContext getContext(){
        return context;
    }

    @Override
    public void start() {
        String configPath = ConfigUtils.getProperty(SPRING_CONFIG);
        if (StringUtils.isEmpty(configPath)){
            configPath = DEFAULT_SPRING_CONFIG;
        }
        context = new ClassPathXmlApplicationContext(configPath.split("[,\\s]+"));
        context.start();
    }

    @Override
    public void stop() {
        try {
            if (!Objects.isNull(context)){
                context.stop();
                context.close();
                context = null;
            }
        }catch (Throwable t){
            logger.error(t.getMessage(), t);
        }
    }
}
