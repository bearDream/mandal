package cn.ching.mandal.config;

import cn.ching.mandal.common.status.StatusChecker;
import cn.ching.mandal.common.threadpool.ThreadPool;
import cn.ching.mandal.config.support.Parameter;
import lombok.Getter;
import lombok.Setter;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProviderConfig extends AbstractServiceConfig{

    private static final long serialVersionUID = 3670142951113261816L;

    // service IP address
    private String host;

    // serivice port
    private Integer port;

    private String contextPath;

    private String threadpool;

    @Getter
    @Setter
    private Integer threads;

    @Getter
    @Setter
    private Integer iothreads;

    @Getter
    @Setter
    private Integer queues;

    @Getter
    @Setter
    private Integer accepts;

    @Getter
    @Setter
    private String codec;

    @Getter
    @Setter
    private String charset;

    // payload max length
    @Getter
    @Setter
    private Integer payload;

    @Getter
    @Setter
    private Integer buffer;

    @Getter
    @Setter
    private String network;

    @Getter
    @Setter
    private String server;

    @Getter
    @Setter
    private String client;

    private String prompt;

    private String status;

    @Getter
    @Setter
    private Integer wait;

    private Boolean isDefault;

    @Parameter(exclude = true)
    public Boolean isDefault(){
        return this.isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Parameter(exclude = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Parameter(exclude = true)
    public Integer getPort() {
        return this.port;
    }

    @Parameter(exclude = true)
    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        checkPathName("contextpath", contextPath);
        this.contextPath = contextPath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        checkExtension(ThreadPool.class, "threadpool", threadpool);
        this.threadpool = threadpool;
    }

    @Parameter(escaped = true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        checkMultiExtension(StatusChecker.class, "status", status);
        this.status = status;
    }

    @Override
    public String getCluster() {
        return super.getCluster();
    }

    @Override
    public Integer getConnections() {
        return super.getConnections();
    }

    @Override
    public Integer getTimeout() {
        return super.getTimeout();
    }

    @Override
    public Integer getRetries() {
        return super.getRetries();
    }

    @Override
    public String getLoadBalance() {
        return super.getLoadBalance();
    }

    @Override
    public Boolean getAsync() {
        return super.getAsync();
    }

    @Override
    public Integer getActives() {
        return super.getActives();
    }


}
