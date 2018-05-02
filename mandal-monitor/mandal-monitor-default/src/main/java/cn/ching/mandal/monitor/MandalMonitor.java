package cn.ching.mandal.monitor;

import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.monitor.api.Monitor;
import cn.ching.mandal.monitor.api.MonitorService;
import cn.ching.mandal.rpc.Invoker;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 2018/3/13
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalMonitor implements Monitor{

    private static final Logger logger = LoggerFactory.getLogger(MandalMonitor.class);

    private static final int LENGTH = 10;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4, new NamedThreadFactory("MandalMonitorSendTimer", true));

    private final ScheduledFuture<?> sendFuture;

    private final Invoker<MonitorService> monitorInvoker;

    private final MonitorService monitorService;

    private final long monitorInterval;

    private final ConcurrentMap<Statistics, AtomicReference<long[]>> statisticMap = new ConcurrentHashMap<>();

    public MandalMonitor(Invoker<MonitorService> monitorInvoker, MonitorService monitorService){
        this.monitorInvoker = monitorInvoker;
        this.monitorService = monitorService;
        this.monitorInterval = monitorInvoker.getUrl().getPositiveParameter("interval", 60000);

        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                send();
            }catch (Throwable t){
                logger.error("Unexpected error occur at send statistic, cause by " + t.getMessage(), t);
            }
        }, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);
    }

    public void send() {

        logger.info("Send statistics to monitor " + getUrl());

        String timestamp = String.valueOf(System.currentTimeMillis());
        for (Map.Entry<Statistics, AtomicReference<long[]>> entry : statisticMap.entrySet()) {

            // get static data.
            Statistics statistics = entry.getKey();
            AtomicReference<long[]> reference = entry.getValue();
            long[] numbers = reference.get();
            long success = numbers[0];
            long failure = numbers[1];
            long input = numbers[2];
            long output = numbers[3];
            long elapsed = numbers[4];
            long concurrent = numbers[5];
            long maxInput = numbers[6];
            long maxOutput = numbers[7];
            long maxElapsed = numbers[8];
            long maxConcurrent = numbers[9];

            // send static data.
            URL url = statistics.getUrl().addParameters(MonitorService.TIMESTAMP, timestamp,
                    MonitorService.SUCCESS, String.valueOf(success),
                    MonitorService.FAILURE, String.valueOf(failure),
                    MonitorService.INPUT, String.valueOf(input),
                    MonitorService.OUTPUT, String.valueOf(output),
                    MonitorService.ELAPSED, String.valueOf(elapsed),
                    MonitorService.CONCURRENT, String.valueOf(concurrent),
                    MonitorService.MAX_INPUT, String.valueOf(maxInput),
                    MonitorService.MAX_OUTPUT, String.valueOf(maxOutput),
                    MonitorService.MAX_ELAPSED, String.valueOf(maxElapsed),
                    MonitorService.MAX_CONCURRENT, String.valueOf(maxConcurrent));
            monitorService.collect(url);

            // reset
            long[] current;
            long[] update = new long[LENGTH];
            do {
                current = reference.get();
                if (Objects.isNull(current)){
                    update[0] = 0;
                    update[1] = 0;
                    update[2] = 0;
                    update[3] = 0;
                    update[4] = 0;
                    update[5] = 0;
                }else {
                    update[0] = current[0] - success;
                    update[1] = current[1] - failure;
                    update[2] = current[2] - input;
                    update[3] = current[3] - output;
                    update[4] = current[4] - elapsed;
                    update[5] = current[5] - concurrent;
                }
            }while (!reference.compareAndSet(current, update));
        }

    }

    @Override
    public void collect(URL url) {

        // data to collect from url.
        int success = url.getParameter(MonitorService.SUCCESS, 0);
        int failure = url.getParameter(MonitorService.FAILURE, 0);
        int input = url.getParameter(MonitorService.INPUT, 0);
        int output = url.getParameter(MonitorService.OUTPUT, 0);
        int elapsed = url.getParameter(MonitorService.ELAPSED, 0);
        int concurrent = url.getParameter(MonitorService.CONCURRENT, 0);

        // init atomic reference.
        Statistics statistics = new Statistics(url);
        AtomicReference<long[]> reference = statisticMap.get(statistics);
        if (Objects.isNull(reference)){
            statisticMap.putIfAbsent(statistics, new AtomicReference<>());
            reference = statisticMap.get(statistics);
        }

        // use CompareAndSet to sum.
        long[] current;
        long[] update = new long[LENGTH];
        do {

            current = reference.get();
            if (Objects.isNull(current)){
                update[0] = success;
                update[1] = failure;
                update[2] = input;
                update[3] = output;
                update[4] = elapsed;
                update[5] = concurrent;
                update[6] = input;
                update[7] = output;
                update[8] = elapsed;
                update[9] = concurrent;
            }else {
                update[0] = current[0] + success;
                update[1] = current[1] + failure;
                update[2] = current[2] + input;
                update[3] = current[3] + output;
                update[4] = current[4] + elapsed;
                update[5] = (current[5] + concurrent) / 2;
                update[6] = current[6] > input ? current[6] : input;
                update[7] = current[7] > output ? current[7] : output;
                update[8] = current[8] > elapsed ? current[8] : elapsed;
                update[9] = current[9] > concurrent ? current[9] : concurrent;
            }
        }while (!reference.compareAndSet(current, update));
    }

    @Override
    public URL getUrl() {
        return monitorInvoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return monitorInvoker.isAvailable();
    }

    @Override
    public void destroy() {
        try {
            sendFuture.cancel(true);
        }catch (Throwable t){
            logger.error("Unexpected error occur at cancel sender timer, cause by " + t.getMessage(), t);
        }
    }

    @Override
    public List<URL> lookup(URL query) {
        return monitorService.lookup(query);
    }
}
