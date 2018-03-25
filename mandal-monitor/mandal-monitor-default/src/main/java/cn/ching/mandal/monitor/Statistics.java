package cn.ching.mandal.monitor;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.monitor.api.MonitorService;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 2018/3/13
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class Statistics implements Serializable {
    private static final long serialVersionUID = 1521092055847896569L;

    @Getter
    @Setter
    private URL url;

    @Getter
    private String application;

    @Getter
    private String service;

    @Getter
    private String method;

    @Getter
    @Setter
    private String group;

    @Setter
    @Getter
    private String version;

    @Getter
    private String client;

    @Getter
    private String server;

    public Statistics(URL url){
        this.url = url;
        this.application = url.getParameter(MonitorService.APPLICATION);
        this.service = url.getParameter(MonitorService.INTERFACE);
        this.method = url.getParameter(MonitorService.METHOD);
        this.group = url.getParameter(MonitorService.GROUP);
        this.version = url.getParameter(MonitorService.VERSION);
        this.client = url.getParameter(MonitorService.CONSUMER, url.getAddress());
        this.server = url.getParameter(MonitorService.PROVIDER, url.getAddress());
    }

    public Statistics setApplication(String application) {
        this.application = application;
        return this;
    }

    public Statistics setService(String service) {
        this.service = service;
        return this;
    }

    public Statistics setClient(String client) {
        this.client = client;
        return this;
    }

    public Statistics setServer(String server) {
        this.server = server;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Statistics that = (Statistics) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (application != null ? !application.equals(that.application) : that.application != null) return false;
        if (service != null ? !service.equals(that.service) : that.service != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (group != null ? !group.equals(that.group) : that.group != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (client != null ? !client.equals(that.client) : that.client != null) return false;
        return server != null ? server.equals(that.server) : that.server == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (application != null ? application.hashCode() : 0);
        result = 31 * result + (service != null ? service.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (client != null ? client.hashCode() : 0);
        result = 31 * result + (server != null ? server.hashCode() : 0);
        return result;
    }
}
