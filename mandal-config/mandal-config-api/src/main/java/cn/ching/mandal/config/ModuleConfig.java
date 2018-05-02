package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.support.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 2018/3/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ModuleConfig extends AbstractConfig {

    private static final long serialVersionUID = 7636710397104114316L;

    private String name;

    @Setter
    private String version;

    @Getter
    private String owner;

    @Getter
    private String organization;

    // registry centers
    @Getter
    private List<RegistryConfig> registries;

    // monitor center
    @Getter
    @Setter
    private MonitorConfig monitor;

    @Getter
    @Setter
    private Boolean isDefault;

    public ModuleConfig(){}

    public ModuleConfig(String name){this.name = name;}

    @Parameter(key = "module", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (StringUtils.isEmpty(id)){
            id = name;
        }
    }

    @Parameter(key = Constants.MODULE_VERSION)
    public String getVersion() {
        return version;
    }

    public void setOwner(String owner) {
        checkName("owner", owner);
        this.owner = owner;
    }

    public void setOrganization(String organization) {
        checkName("organization", organization);
        this.organization = organization;
    }

    public RegistryConfig getRegistrys() {
        return CollectionUtils.isEmpty(registries) ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<>();
        registries.add(registry);
        this.registries = registries;
    }

    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    public void setMonitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
    }
}
