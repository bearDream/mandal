package cn.ching.mandal.monitor;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.monitor.api.Monitor;
import cn.ching.mandal.monitor.api.MonitorService;
import cn.ching.mandal.monitor.api.support.AbstractMonitorFactory;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.Protocol;
import cn.ching.mandal.rpc.ProxyFactory;
import lombok.Setter;

import java.util.Objects;

/**
 * 2018/3/13
 * Mandal monitor Factory.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalMonitorFactory extends AbstractMonitorFactory{

    @Setter
    private Protocol protocol;

    @Setter
    private ProxyFactory proxyFactory;

    private static final String defaultProtocol = "mandal";

    @Override
    protected Monitor createMonitor(URL url) {

        url = url.setProtocol(url.getParameter(Constants.PROTOCOL_KEY, defaultProtocol));
        if (Objects.isNull(url.getPath()) || url.getPath().length() == 0){
            url = url.setPath(MonitorService.class.getName());
        }
        String filter = url.getParameter(Constants.REFERENCE_FILTER_KEY);
        if (Objects.isNull(filter) || filter.length() == 0){
            filter = "";
        }else {
            filter = filter + ",";
        }
        url = url.addParameters(Constants.CLUSTER_KEY, "failsafe", Constants.CHECK_KEY, String.valueOf(false), Constants.REFERENCE_FILTER_KEY, filter + "-monitor");
        Invoker<MonitorService> monitorInvoker = protocol.refer(MonitorService.class, url);
        MonitorService monitorService = proxyFactory.getProxy(monitorInvoker);
        return new MandalMonitor(monitorInvoker, monitorService);
    }
}
