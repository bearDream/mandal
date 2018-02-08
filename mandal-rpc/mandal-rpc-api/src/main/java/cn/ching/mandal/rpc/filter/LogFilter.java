package cn.ching.mandal.rpc.filter;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConcurrentHashSet;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.rpc.*;
import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 2018/1/12
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate(group = Constants.PROVIDER, value = Constants.ACCESS_LOG_KEY)
public class LogFilter implements Filter{

    private static Logger logger = LoggerFactory.getLogger(LogFilter.class);

    private final String DATE_FORMATTER = "yyyy-MM-dd hh:mm:ss";

    private final String ACCESS_LOG_KEY = "mandal.accesslog";

    private volatile ScheduledFuture<?> future = null;

    private static final long LOG_OUTPUT_INTERVAL = 5000;

    private static final long LOG_BUFFER_SIZE = 5000;

    private final ConcurrentHashMap<String, Set<String>> logQue = new ConcurrentHashMap<>();

    private final ScheduledExecutorService logScheduled = new ScheduledThreadPoolExecutor(5, new NamedThreadFactory());

    @Override
    public Result invoker(Invoker<?> invoker, Invocation invocation) {
        try {
            String log = invoker.getUrl().getParameter(Constants.ACCESS_LOG_KEY);
            if (ConfigUtils.isNotEmpty(log)){
                RpcContext context = RpcContext.getContext();
                String serviceName = invoker.getInterface().getName();
                String version = invoker.getUrl().getParameter(Constants.VERSION_KEY);
                String group = invoker.getUrl().getParameter(Constants.GROUP_KEY);
                StringBuilder logString = new StringBuilder();
                logString.append("[").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMATTER))).append("]");
                logString.append(" context remote hostname:").append(context.getRemoteHost()).append(":").append(context.getRemotePort());
                logString.append("-> ").append(" local hostname:").append(context.getLocalHost()).append(":").append(context.getLocalPort());
                if (!StringUtils.isBlank(group)){
                    logString.append(group).append("/");
                }
                logString.append(serviceName);
                if (!StringUtils.isBlank(version)){
                    logString.append(":").append(version);
                }
                logString.append(" ");
                logString.append(invocation.getMethodName());
                logString.append("(");
                Class<?>[] types = invocation.getParameterTypes();
                if (Objects.nonNull(types) && types.length > 0){
                    for (Class<?> type : types){
                        if (Objects.nonNull(type)){
                            logString.append(type).append(" ,");
                        }
                    }
                    // delete last ","
                    logString.substring(0, logString.length()-1);
                }
                logString.append(")");
                Object[] argument = invocation.getArguments();
                if (Objects.nonNull(argument) && argument.length > 0){
                    logString.append(JSON.toJSONString(argument));
                }
                String msg = logString.toString();
                // if log is default, then output by logger, otherwise output to file
                if (ConfigUtils.isDefault(log)){
                    LoggerFactory.getLogger(ACCESS_LOG_KEY + "." + invoker.getInterface().getName()).info(msg);
                }else {
                    logToFile(log, msg);
                }

            }
        }catch (Exception e){
            logger.warn("accessLogFilter has exception of service[" + invoker + "->" + invocation, e);
        }
        return invoker.invoke(invocation);
    }

    private void init(){
        if (Objects.isNull(future)){
            synchronized (logScheduled){
                if (future == null){
                    future = logScheduled.scheduleWithFixedDelay(new LogTask(), LOG_OUTPUT_INTERVAL, LOG_OUTPUT_INTERVAL, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * bring log to file
     * @param log
     * @param msg
     */
    private void logToFile(String log, String msg) {
        init();
        Set<String> logSet = logQue.get(log);
        if (Objects.isNull(logSet)){
            logQue.putIfAbsent(log, new ConcurrentHashSet<>());
            logSet = logQue.get(log);
        }
        if (logSet.size() < LOG_BUFFER_SIZE){
            logSet.add(msg);
        }
    }

    private class LogTask implements Runnable {
        @Override
        public void run() {
            try {
                if(logQue.size() > 0){
                    for (Map.Entry<String, Set<String>> entry : logQue.entrySet()){
                        try {
                            String accessLog = entry.getKey();
                            Set<String> logSet = entry.getValue();
                            File file = new File(accessLog);
                            File dir = file.getParentFile();
                            if (Objects.nonNull(dir) && !dir.exists()){
                                dir.mkdirs();
                            }
                            if (logger.isDebugEnabled()){
                                logger.debug("log content append to: " + accessLog);
                            }
                            if (file.exists()){
                                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMATTER));
                                String last = new SimpleDateFormat(DATE_FORMATTER).format(file.lastModified());
                                if (!now.equals(last)){
                                    File archive = new File(file.getAbsolutePath() + "." + file);
                                    file.renameTo(archive);
                                }
                            }
                            FileWriter writer = new FileWriter(file, true);
                            try {
                                for (Iterator<String> iterator = logSet.iterator(); iterator.hasNext(); iterator.remove()){
                                    writer.write(iterator.next());
                                    writer.write("\r\n");
                                }
                                writer.flush();
                            }finally {
                                writer.close();
                            }
                        }catch (Exception e){
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }catch (Exception e){
                logger.error(e.getMessage(), e);
            }
        }
    }
}
