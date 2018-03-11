package cn.ching.mandal.config;

import cn.ching.mandal.config.support.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 2018/3/10
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MonitorConfig extends AbstractConfig {

    private static final long serialVersionUID = 5227943535991830116L;

    @Setter
    private String protocol;

    @Setter
    private String address;

    @Setter
    private String username;

    @Setter
    private String password;

    @Getter
    @Setter
    private String group;

    @Getter
    @Setter
    private String version;

    @Getter
    private Map<String, String> parameters;

    @Getter
    @Setter
    private Boolean isDefault;

    public MonitorConfig(){}

    public MonitorConfig(String address){
        this.address = address;
    }

    @Parameter(exclude = true)
    public String getAddress() {
        return address;
    }

    @Parameter(exclude = true)
    public String getProtocol() {
        return protocol;
    }

    @Parameter(exclude = true)
    public String getUsername() {
        return username;
    }

    @Parameter(exclude = true)
    public String getPassword() {
        return password;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }
}
