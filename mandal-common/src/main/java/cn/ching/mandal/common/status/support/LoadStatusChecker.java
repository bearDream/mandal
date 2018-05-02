package cn.ching.mandal.common.status.support;

import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.status.Status;
import cn.ching.mandal.common.status.StatusChecker;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * 2018/3/8
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate
public class LoadStatusChecker implements StatusChecker{

    @Override
    public Status check() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        double load;
        try {
            Method method = OperatingSystemMXBean.class.getMethod("getSystemLoadAverage", new Class<?>[0]);
            load = (double) method.invoke(operatingSystemMXBean, new Object[0]);
        }catch (Throwable t){
            load = -1;
        }
        int cpu = operatingSystemMXBean.getAvailableProcessors();
        return new Status(load < 0 ? Status.Level.UNKNOWN : (load < cpu ? Status.Level.OK : Status.Level.WARN), (load < 0 ? "" : "load: " + load + ",") + " cpu:" + cpu);
    }
}
