package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.compiler.support.AdaptiveCompiler;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.support.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 2018/3/10
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ApplicationConfig extends AbstractConfig {
    private static final long serialVersionUID = -9049676886778894774L;

    private String name;

    @Setter
    private String version;

    @Getter
    private String owner;

    @Getter
    private String organization;

    @Getter
    private String architecture;

    @Getter
    private String environment;

    @Getter
    private String compiler;

    @Getter
    private String logger;

    private List<RegistryConfig> registries;

    @Getter
    @Setter
    private MonitorConfig monitor;

    private Boolean isDefault;

    @Setter
    private String dumpDirectory;

    @Getter
    private Map<String, String> parameters;

    public ApplicationConfig(){

    }


    public ApplicationConfig(String name){

    }

    @Parameter(key = Constants.APPLICATION_KEY, required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (StringUtils.isEmpty(name)){
            id = name;
        }
    }

    @Parameter(key = Constants.APPLICATION_VERSION)
    public String getVersion() {
        return version;
    }

    public void setOwner(String owner) {
        checkMultiName("owner", owner);
        this.owner = owner;
    }

    public void setOrganization(String organization) {
        checkName("organization", organization);
        this.organization = organization;
    }

    public void setArchitecture(String architecture) {
        checkName("architecture", architecture);
        this.architecture = architecture;
    }

    public void setEnvironment(String environment) {
        checkName("environment", environment);
        if (!Objects.isNull(environment)){
            if (!("develop".equals(environment) || "test".equals(environment) || "product".equals(environment))){
                throw new IllegalStateException("Unsupported enviroment: " + environment + ", only support develop/test/product, default is product.");
            }
        }
        this.environment = environment;
    }

    public RegistryConfig getRegistry() {
        return Objects.isNull(registries) || registries.size() == 0 ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<>();
        registries.add(registry);
        this.registries = registries;
    }

    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
        AdaptiveCompiler.setDefaultCompiler(compiler);
    }

    public void setLogger(String logger) {
        this.logger = logger;
        LoggerFactory.setLoggerAdapter(logger);
    }

    public Boolean isDefault(){
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    @Parameter(key = "dump.directory")
    public String getDumpDirectory() {
        return dumpDirectory;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }
}
