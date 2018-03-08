package cn.ching.mandal.registry.status;

import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.status.Status;
import cn.ching.mandal.common.status.StatusChecker;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.registry.Registry;
import cn.ching.mandal.registry.support.AbstractRegistryFactory;

import java.util.Collection;

/**
 * 2018/3/8
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate
public class RegistryStatusCheck implements StatusChecker{


    @Override
    public Status check() {
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        if (CollectionUtils.isEmpty(registries)){
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        StringBuilder sb = new StringBuilder();
        for (Registry registry : registries) {
            if (sb.length() > 0){
                sb.append(",");
            }
            sb.append(registry.getUrl().getAddress());
            if (!registry.isAvailable()){
                level = Status.Level.ERROR;
                sb.append("disconnected");
            }else {
                sb.append("disconnected");
            }
        }
        return new Status(level, sb.toString());
    }
}
