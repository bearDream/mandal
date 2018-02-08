package cn.ching.mandal.container;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConfigUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 2018/1/30
 * mandal main class.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class Mandal {

    public static final String CONTAINER_KEY = "mandal.container";

    public static final String SHUTDOWN_HOOK_KEY = "mandal.shutdown.hook";

    public static final Logger logger = LoggerFactory.getLogger(Mandal.class);

    public static final ExtensionLoader<Container> loader = ExtensionLoader.getExtensionLoader(Container.class);

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static Condition STOP = LOCK.newCondition();

    public static void main(String[] args){
        try {
            if (Objects.isNull(args) || args.length == 0){
                String config = ConfigUtils.getProperty(CONTAINER_KEY, loader.getDefaultExtensionName());
                args = Constants.COMMA_SPLIT_PATTERN.split(config);
            }

            final List<Container> containers = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                containers.add(loader.getExtension(args[i]));
            }
            logger.info("Use Container type(" + Arrays.toString(args) + ") to run mandal service");

            if ("true".equals(System.getProperty(SHUTDOWN_HOOK_KEY))){
                Runtime.getRuntime().addShutdownHook(new Thread(){
                    @Override
                    public void run() {
                        containers.forEach(container -> {
                            try {
                                container.stop();
                                logger.info("Mandal " + container.getClass().getSimpleName() + " stopped.");
                            }catch (Throwable t){
                                logger.error(t.getMessage(), t);
                            }

                            try {
                                LOCK.lock();
                                STOP.signal();
                            } finally {
                                LOCK.unlock();
                            }
                        });
                    }
                });
            }

            containers.forEach(container -> {
                container.start();
                logger.info("Mandal " + container.getClass().getSimpleName() + " started.");
            });
            System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(LocalDate.now()) + " mandal service is started.");

        } catch (RuntimeException e){
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            System.exit(1);
        }

        try {
            LOCK.lock();
            STOP.await();
        } catch (InterruptedException e) {
            logger.warn("Mandal service server stopped, interrupted by other thread.", e);
        }finally {
            LOCK.unlock();
        }
    }
}
