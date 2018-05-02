package cn.ching.mandal.config;

import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.registry.support.AbstractRegistryFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 2018/3/10
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RegistryConfig extends AbstractConfig {

    private static final long serialVersionUID = -837649174324857633L;

    public static final String NO_AVAILABLE = "N/A";

    // register center address
    @Setter
    private String address;

    // login register center username
    @Getter
    private String username;

    // login register center password
    @Getter
    private String password;

    // register center port
    @Getter
    @Setter
    private Integer port;

    // register center protocol
    @Getter
    private String protocol;

    // client implication class
    @Getter
    private String transporter;

    @Getter
    private String server;

    @Getter
    private String client;

    @Getter
    @Setter
    private String cluster;

    @Getter
    @Setter
    private String group;

    @Getter
    @Setter
    private String version;

    // request timeout in millsec for register center
    @Getter
    @Setter
    private Integer timeout;

    // session timeout int millsec for register center
    @Getter
    @Setter
    private Integer session;

    // to save register center dynamic list
    @Getter
    private String file;

    // whether to check if register center is available when boot up
    @Getter
    @Setter
    private Boolean check;

    // whether to allow dynamic service to register on register center
    @Getter
    @Setter
    private Boolean dynamic;

    // whether to export service on register center
    @Getter
    @Setter
    private Boolean register;

    // whether allow to subscribe service on register center
    @Getter
    @Setter
    private Boolean subscribe;

    // customized parameters
    @Getter
    private Map<String, String> parameters;

    @Getter
    @Setter
    private Boolean isDefault;

    public RegistryConfig(){}

    public RegistryConfig(String address){
        this.address = address;
    }

    public static void destroyAll(){
        AbstractRegistryFactory.destroyAll();
    }

    @Parameter(exclude = true)
    public String getAddress() {
        return address;
    }

    public void setProtocol(String protocol) {
        checkName("protocol", protocol);
        this.protocol = protocol;
    }

    public void setUsername(String username) {
        checkName("username", username);
        this.username = username;
    }

    public void setPassword(String password) {
        checkLength("password", password);
        this.password = password;
    }

    public void setFile(String file) {
        checkPathLength("file", file);
        this.file = file;
    }

    public void setTransport(String transporter) {
        checkName("transporter", transporter);
        this.transporter = transporter;
    }

    public void setServer(String server) {
        checkName("server", server);
        this.server = server;
    }

    public void setClient(String client) {
        checkName("client", client);
        this.client = client;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }
}
