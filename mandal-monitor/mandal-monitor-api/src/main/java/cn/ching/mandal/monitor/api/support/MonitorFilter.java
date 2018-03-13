package cn.ching.mandal.monitor.api.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.monitor.api.Monitor;
import cn.ching.mandal.monitor.api.MonitorFactory;
import cn.ching.mandal.monitor.api.MonitorService;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.support.RpcUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2018/3/13
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER})
public class MonitorFilter implements Filter{

    private static final Logger logger = LoggerFactory.getLogger(MonitorFilter.class);

    private final ConcurrentMap<String, AtomicInteger> concurrents = new ConcurrentHashMap<>();

    private MonitorFactory monitorFactory;

    public void setMonitorFactory(MonitorFactory monitorFactory) {
        this.monitorFactory = monitorFactory;
    }


    @Override
    public Result invoker(Invoker<?> invoker, Invocation invocation) {
        if (invoker.getUrl().hasParameter(Constants.MONITOR_KEY)){
            RpcContext context = RpcContext.getContext();
            String remoteHost = context.getRemoteHost();
            Long start = System.currentTimeMillis();
            getConcurrent(invoker, invocation).incrementAndGet();
            try {
                Result result = invoker.invoke(invocation);
                collect(invoker, invocation, result, remoteHost, start, false);
                return result;
            }catch (RpcException e){
                collect(invoker, invocation, null, remoteHost, start, true);
                throw e;
            }finally {
                getConcurrent(invoker, invocation).decrementAndGet();
            }
        }else {
            return invoker.invoke(invocation);
        }
    }

    private void collect(Invoker<?> invoker, Invocation invocation, Result result, String remoteHost, Long start, Boolean error) {
        try {
            Long elapsed = System.currentTimeMillis();
            int concurrent = getConcurrent(invoker, invocation).get();
            String app = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
            String service = invoker.getInterface().getName();
            String method = RpcUtils.getMethodName(invocation);
            URL url = invoker.getUrl().getUrlParameter(Constants.MONITOR_KEY);
            Monitor monitor = monitorFactory.getMonitor(url);

            if (Objects.isNull(monitor)){
                return;
            }
            int localPort;
            String remoteKey;
            String removeValue;
            if (Constants.CONSUMER_SIDE.equals(invoker.getUrl().getParameter(Constants.SIDE_KEY))){
                // serivce consumer;
                localPort = 0;
                remoteKey = MonitorService.PROVIDER;
                removeValue = invoker.getUrl().getAddress();
            }else {
                // service provider
                localPort = invoker.getUrl().getPort();
                remoteKey = MonitorService.CONSUMER;
                removeValue = remoteHost;
            }
            String input = "";
            String output = "";
            if (!Objects.isNull(invocation.getAttachment(Constants.INPUT_KEY))){
                input = invocation.getAttachment(Constants.INPUT_KEY);
            }
            if (!Objects.isNull(result) && !Objects.isNull(result.getAttachment(Constants.OUTPUT_KEY))){
                output = result.getAttachment(Constants.OUTPUT_KEY);
            }
            monitor.collect(new URL(Constants.COUNT_PROTOCOL,
                    NetUtils.getLocalHost(),
                    localPort,
                    service + "/" + method,
                    MonitorService.APPLICATION,
                    app,
                    MonitorService.METHOD,
                    method,
                    remoteKey,
                    removeValue,
                    error ? MonitorService.FAILURE : MonitorService.SUCCESS,
                    "1",
                    MonitorService.ELAPSED,
                    String.valueOf(elapsed),
                    MonitorService.CONCURRENT,
                    String.valueOf(concurrent),
                    Constants.INPUT_KEY,
                    input,
                    Constants.OUTPUT_KEY,
                    output));
        }catch (Throwable t){
            logger.error("Failed to monitor count service " + invoker.getUrl() + ", cause: " + t.getMessage(), t);
        }
    }

    private AtomicInteger getConcurrent(Invoker<?> invoker, Invocation invocation) {
        String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
        AtomicInteger concurrent = concurrents.get(key);
        if (Objects.isNull(concurrent)){
            concurrents.putIfAbsent(key, new AtomicInteger());
            concurrent = concurrents.get(key);
        }
        return concurrent;
    }
}
