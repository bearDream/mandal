package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.serialize.Serialization;
import cn.ching.mandal.common.threadpool.ThreadPool;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.registry.support.AbstractRegistryFactory;
import cn.ching.mandal.rpc.Protocol;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProtocolConfig extends AbstractConfig {

    private static final long serialVersionUID = -6410766149709713527L;

    protected String name;

    private String host;

    private Integer port;

    private String contextpath;

    private String threadpool;

    // thread pool size(fixed size)
    @Getter
    @Setter
    private Integer threads;

    // IO thread pool size
    @Getter
    @Setter
    private Integer iothreads;

    // thread pool queue size
    @Getter
    @Setter
    private Integer queues;

    // max acceptable connections.
    @Getter
    @Setter
    private Integer accepts;

    @Getter
    @Setter
    private String codec;

    @Getter
    private String serialization;

    private String charset;

    // payload max length
    private Integer payload;

    // buffer size
    private Integer buffer;

    // heartbeat interval
    private Integer hearbeat;

    private String accesslog;

    private String transport;

    private String exchange;

    // thread dispatcher mode.
    private String dispatcher;

    private String network;

    private String server;

    private String client;

    // supported telnet command.
    private String telnet;

    // command line prompt
    private String prompt;

    // status check.
    private String status;

    // whether to register
    private Boolean register;

    private String extension;

    private Map<String, String> parameters;

    @Getter
    @Setter
    private Boolean isDefault;

    private static final AtomicBoolean destroyed = new AtomicBoolean(false);

    public ProtocolConfig(){

    }

    public ProtocolConfig(String name){
        setName(name);
    }

    public ProtocolConfig(String name, int port){
        setName(name);
        setPort(port);
    }

    public static void destroyAll(){
        if (!destroyed.compareAndSet(false, true)){
            return;
        }
        AbstractRegistryFactory.destroyAll();
        ExtensionLoader<Protocol> loader = ExtensionLoader.getExtensionLoader(Protocol.class);
        for (String protocalName : loader.getLoadedExtension()) {
            try {
                Protocol protocol = loader.getLoadedExtension(protocalName);
                if (Objects.isNull(protocol)){
                    protocol.destroy();
                }
            }catch (Throwable t){
                logger.warn(t.getMessage(), t);
            }
        }
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (StringUtils.isEmpty(id)){
            id = name;
        }
    }

    @Parameter(exclude = true)
    public String getName() {
        return name;
    }

    @Parameter(exclude = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        checkName("host", host);
        this.host = host;
    }

    @Parameter(exclude = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Parameter(exclude = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        checkPathName("contextpath", contextpath);
        this.contextpath = contextpath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        checkExtension(ThreadPool.class, "threadpool", threadpool);
        this.threadpool = threadpool;
    }

    public void setSerialization(String serialization) {
        if (Constants.DEFAULT_KEY.equals(name)){
            checkMultiExtension(Serialization.class, "serialization", serialization);
        }
        this.serialization = serialization;
    }
}
