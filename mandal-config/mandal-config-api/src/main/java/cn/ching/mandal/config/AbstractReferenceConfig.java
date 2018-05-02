package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.rpc.support.ProtocolUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractReferenceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = 6823564859261129093L;

    // ====== Reference config default val. If not set reference attribute, then this default value will take effect

    @Getter
    @Setter
    protected Boolean check;

    @Getter
    @Setter
    protected Boolean init;

    // whether use generic interface.
    @Getter
    @Setter
    protected String generic;

    // lazy create connection.
    @Setter
    protected Boolean lazy;

    @Setter
    protected String reconnect;

    @Setter
    protected String sticky;

    protected Boolean stubevent;

    @Getter
    protected String version;

    @Getter
    protected String group;

    @Parameter(exclude = true)
    public Boolean isGeneric(){
        return ProtocolUtils.isGeneric(generic);
    }

    public void setGeneric(Boolean generic) {
        if (!Objects.isNull(generic)){
            this.generic = generic.toString();
        }
    }

    public void setGeneric(String generic) {
        this.generic = generic.toString();
    }

    @Parameter(key = Constants.REFERENCE_FILTER_KEY, append = true)
    @Override
    public String getFilter() {
        return super.getFilter();
    }

    @Parameter(key = Constants.INVOKER_LISTENER_KEY, append = true)
    @Override
    public String getListener() {
        return super.getListener();
    }

    @Parameter(key = Constants.INVOKER_LISTENER_KEY, append = true)
    @Override
    public void setListener(String listener) {
        super.setListener(listener);
    }

    @Parameter(key = Constants.LAZY_CONNECT_KEY, append = true)
    public Boolean getLazy() {
        return lazy;
    }

    @Override
    public void setOnconnect(String onconnect) {
        if (!StringUtils.isEmpty(onconnect)){
            this.stubevent = true;
        }
        super.setOnconnect(onconnect);
    }

    @Parameter(key = Constants.STUB_EVENT_KEY)
    public Boolean getStubevent() {
        return stubevent;
    }

    @Parameter(key = Constants.RECONNECT_KEY)
    public String getReconnect() {
        return reconnect;
    }

    @Parameter(key = Constants.CLUSTER_STICKY_KEY)
    public String getSticky() {
        return sticky;
    }

    public void setVersion(String version) {
        checkKey("version", version);
        this.version = version;
    }

    public void setGroup(String group) {
        checkKey("group", group);
        this.group = group;
    }
}
