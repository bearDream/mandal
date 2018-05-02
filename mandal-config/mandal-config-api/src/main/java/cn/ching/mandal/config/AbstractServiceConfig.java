package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.rpc.ExporterListener;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AbstractServiceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = 985741092375478032L;

    @Getter
    protected String version;

    @Getter
    protected String group;

    // whether the service is deprecated.
    @Getter
    @Setter
    protected Boolean deprecated;

    // delay service exporting.
    @Setter
    @Getter
    protected Integer delay;

    // whether export the service
    @Setter
    @Getter
    protected Boolean export;

    @Setter
    @Getter
    protected Integer weight;

    @Setter
    protected String document;

    // whether to register as a dynamic service or not on register center.
    @Getter
    @Setter
    protected Boolean dynamic;

    // whether use token
    @Getter
    protected String token;

    @Getter
    protected String accesslog;

    @Getter
    protected List<ProtocolConfig> protocols;

    // max allowed execute times
    @Getter
    @Setter
    protected Integer executes;
    // whether to register
    @Getter
    @Setter
    protected Boolean register;

    @Getter
    @Setter
    private Integer warmup;

    @Getter
    @Setter
    private String serilization;

    public void setVersion(String version) {
        checkKey("version", version);
        this.version = version;
    }

    public void setGroup(String group) {
        checkKey("group", group);
        this.group = group;
    }

    @Parameter(escaped = true)
    public String getDocument() {
        return document;
    }

    public void setToken(String token) {
        if (null == token){
            setToken((String) null);
        }else {
            setToken(String.valueOf(token));
        }
    }

    public void setProtocols(List<? extends ProtocolConfig> protocols) {
        this.protocols = (List<ProtocolConfig>) protocols;
    }

    public ProtocolConfig getProtocol(){
        return CollectionUtils.isEmpty(protocols) ? null : protocols.get(0);
    }

    public void setProtocol(ProtocolConfig protocol){
        this.protocols = Arrays.asList(new ProtocolConfig[]{protocol});
    }

    public void setAccessLog(String accessLog){
        this.accesslog = accessLog;
    }

    public void setAccessLog(Boolean accessLog){
        if (Objects.isNull(accessLog)){
            setAccessLog((String) null);
        }else {
            setAccessLog(String.valueOf(accessLog));
        }
    }

    @Parameter(key = Constants.SERVICE_FILTER_KEY, append = true)
    @Override
    public String getFilter() {
        return super.getFilter();
    }

    @Parameter(key = Constants.EXPORTER_LISTENER_KEY, append = true)
    @Override
    public String getListener() {
        return super.getListener();
    }

    @Override
    public void setListener(String listener) {
        checkMultiExtension(ExporterListener.class, "listener", listener);
        super.setListener(listener);
    }


}
