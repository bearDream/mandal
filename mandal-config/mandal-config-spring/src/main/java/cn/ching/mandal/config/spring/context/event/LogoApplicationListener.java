package cn.ching.mandal.config.spring.context.event;

import cn.ching.mandal.common.Version;
import cn.ching.mandal.common.utils.MandalLogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

import static cn.ching.mandal.config.spring.utils.MandalUtils.LINE_SEPARATOR;
import static cn.ching.mandal.config.spring.utils.MandalUtils.MANDAL_GIT_URL;

/**
 * @author bearDream
 * @email: laxzhang@outlook.com
 * @description: mandal logo generate.
 */
@Order(LoggingApplicationListener.DEFAULT_ORDER+1)
public class LogoApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {


    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        final Logger logger = LoggerFactory.getLogger(getClass());

        String bannerText = buildBannerText();

        if (logger.isInfoEnabled()) {
            logger.info(bannerText);
        } else {
            System.out.print(bannerText);
        }
    }

    String buildBannerText() {

        StringBuilder bannerTextBuilder = new StringBuilder();

        bannerTextBuilder
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR)
                .append(MandalLogo.mandal)
                .append(LINE_SEPARATOR)
                .append(" :: Mandal (v").append(Version.getVersion()).append(") : ")
                .append(MANDAL_GIT_URL)
                .append(LINE_SEPARATOR)
        ;

        return bannerTextBuilder.toString();

    }
}
