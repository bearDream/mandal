package cn.ching.mandal.common.threadpool.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.JVMUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 2018/3/21
 * Log warn info when abort
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy{

    protected static final Logger logger = LoggerFactory.getLogger(AbortPolicyWithReport.class);

    private final String threadName;

    private final URL url;

    private static volatile long lastPrintTime = 0;

    private static Semaphore guard = new Semaphore(1);

    public AbortPolicyWithReport(String threadName, URL url){
        this.threadName = threadName;
        this.url = url;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        String msg = String.format("Thread pool is EXHAUSTED!" + "Thread Name %s, Pool size: %d (active: %d, core: %d," +
                            " max: %d, largest: %d), Task: %d(completed: %d), Exector status:(isShutdown:%s, isTerminated:%s), in %s://%s:%d!",
                                threadName, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(), e.getLargestPoolSize(),
                                e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating(),
                                url.getProtocol(), url.getIp(), url.getPort());
        logger.warn(msg);
        dumpJStack();
        throw new RejectedExecutionException(msg);
    }

    private void dumpJStack(){
        long now = System.currentTimeMillis();

        // dump every 10 minutes.
        if (now - lastPrintTime < Duration.ofMinutes(10).toMillis()){
            return;
        }

        if (!guard.tryAcquire()){
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            String dumpPath = url.getParameter(Constants.DUMP_DIRECTORY, System.getProperty("user.home"));

            DateTimeFormatter sdf;

            String OS = System.getProperty("os.name").toLowerCase();

            if (OS.contains("win")){
                sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            }else {
                sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
            }

            String dateStr = LocalDateTime.now().format(sdf);
            FileOutputStream jstackStream = null;

            try {
                jstackStream = new FileOutputStream(new File(dumpPath, "Mandal_JStack.log" + "." + dateStr));
                JVMUtil.jstack(jstackStream);
            }catch (Throwable t){
                logger.error("dump jstack error.", t);
            }finally {
                guard.release();
                if (jstackStream != null){
                    try {
                        jstackStream.flush();
                        jstackStream.close();
                    }catch (IOException e){

                    }
                }
            }
            lastPrintTime = System.currentTimeMillis();
        });
    }
}
