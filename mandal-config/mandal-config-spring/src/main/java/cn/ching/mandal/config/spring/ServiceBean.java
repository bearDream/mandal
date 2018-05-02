package cn.ching.mandal.config.spring;

import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.config.*;
import cn.ching.mandal.config.annoatation.Service;
import cn.ching.mandal.config.spring.extension.SpringExtensionFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 2018/3/9
 * config Service Provider Bean as Spring Bean.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>{


    private static final long serialVersionUID = -2100186334271486722L;

    @Getter
    private static transient ApplicationContext SPRING_CONTEXT;

    @Getter
    private final transient Service service;

    private transient ApplicationContext applicationContext;

    @Setter
    private transient String beanName;

    private transient boolean supportApplicationListener;

    public ServiceBean(){
        super();
        this.service = null;
    }

    public ServiceBean(Service service){
        super();
        this.service = service;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
        if (!Objects.isNull(applicationContext)){
            SPRING_CONTEXT = applicationContext;
            try {

                Method method = applicationContext.getClass().getMethod("addApplicationListener", new Class<?>[]{ApplicationListener.class});
                method.invoke(applicationContext, new Object[]{this});
                supportApplicationListener = true;
            } catch (Throwable t) {
                if (applicationContext instanceof AbstractApplicationContext){
                    try {
                        Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener", new Class[]{ApplicationListener.class});
                        if (!method.isAccessible()){
                            method.setAccessible(true);
                        }
                        method.invoke(applicationContext, new Object[]{this});
                        supportApplicationListener = true;
                    }catch (Throwable t2){
                    }
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        unexport();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (isDelay() && !isExported() && !isUnexported()){
            if (logger.isInfoEnabled()){
                logger.info("The service has been started by spring. service: " + getInterface());
            }
            export();
        }
    }

    private boolean isDelay(){
        Integer delay = getDelay();
        ProviderConfig provider = getProvider();
        if (Objects.isNull(delay) && !Objects.isNull(provider)){
            delay = provider.getDelay();
        }
        return supportApplicationListener && (delay == null || delay == -1);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // Provider -->> null
        if (getProvider() == null){
            // get all ProviderConfig bean(include subclass) by applicationContext
            Map<String, ProviderConfig> providerConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class, false, false);
            if (!CollectionUtils.isEmpty(providerConfigMap)){
                // get all ProtocolConfig bean(include subclass) by applicationContext
                Map<String, ProtocolConfig> protocolConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
                if (CollectionUtils.isEmpty(protocolConfigMap) && providerConfigMap.size() > 1) {
                    logger.error("mandal not support more provider config. Please config one provider config. provider configs:" + providerConfigMap.values());
                }else {
                    ProviderConfig providerConfig = null;
                    for (ProviderConfig config : providerConfigMap.values()) {
                        if (Objects.isNull(config) || config.isDefault().booleanValue()){
                            if (!Objects.isNull(providerConfig)){
                                throw new IllegalStateException("");
                            }
                            providerConfig = config;
                        }
                    }
                    if (!Objects.isNull(providerConfig)){
                        setProvider(providerConfig);
                    }
                }
            }
        }

        // Application && (Provider || Provider.Application) -->> null
        if (getApplication() == null && (getProvider() == null || getProvider().getApplication() == null)){

            Map<String, ApplicationConfig> applicationConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
            if (!CollectionUtils.isEmpty(applicationConfigMap)){
                ApplicationConfig applicationConfig = null;
                for (ApplicationConfig config : applicationConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()){
                        if (!Objects.isNull(applicationConfig)){
                            throw new IllegalStateException("Duplicate application configs: " + applicationConfig + "and" + config);
                        }
                        applicationConfig = config;
                    }
                }
                if (!Objects.isNull(applicationConfig)){
                    setApplication(applicationConfig);
                }
            }
        }

        // Module && (Provider || Provider.Module)  -->>  null
        if (getModule() == null && (getProvider() == null || getProvider().getModule() == null)){
            Map<String, ModuleConfig> moduleConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class, false, false);
            if (!CollectionUtils.isEmpty(moduleConfigMap)){
                ModuleConfig moduleConfig = null;
                for (ModuleConfig config : moduleConfigMap.values()) {
                    if (config.getIsDefault() == null || config.getIsDefault().booleanValue()){
                        if (!Objects.isNull(moduleConfig)){
                            throw new IllegalStateException("Duplicate module configs: " + moduleConfig + " and " + config);
                        }
                        moduleConfig = config;
                    }
                }
                if (!Objects.isNull(moduleConfig)){
                    setModule(moduleConfig);
                }
            }
        }

        // Registries && (Provider || Provider.Registries) && (Application || Application.Registries)  -->> null and empty
        if (getRegistries() == null
                && (getProvider() == null || getProvider().getRegistries() == null)
                && (getApplication() == null || CollectionUtils.isEmpty(getApplication().getRegistries()))){

            Map<String, RegistryConfig> registryConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (!CollectionUtils.isEmpty(registryConfigMap)){
                List<RegistryConfig> registryConfigs = new ArrayList<>();
                for (RegistryConfig config : registryConfigMap.values()) {
                    if (config.getIsDefault() == null || config.getIsDefault().booleanValue()){
                        registryConfigs.add(config);
                    }
                }
                if (!CollectionUtils.isEmpty(registryConfigs)){
                    setRegistries(registryConfigs);
                }
            }
        }


        // Monitor  Provider  Provider.Monitor  Application  Application.Monitor  -->>  null
        if (getMonitor() == null
                && (getProvider() == null || getProvider().getMonitor() == null)
                && (getApplication() == null || getApplication().getMonitor() == null)){

            Map<String, MonitorConfig> monitorConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class, false, false);
            if (!CollectionUtils.isEmpty(monitorConfigMap)){
                MonitorConfig monitorConfig = null;
                for (MonitorConfig config : monitorConfigMap.values()) {
                    if (config.getIsDefault() == null || config.getIsDefault().booleanValue()){
                        if (!Objects.isNull(config)){
                            throw new IllegalStateException("Duplicate monitor configs: " + monitorConfig + " and " + config);
                        }
                        monitorConfig = config;
                    }
                }
                if (!Objects.isNull(monitorConfig)){
                    setMonitor(monitorConfig);
                }
            }
        }


        // Protocols  Provider  Provider.Protocols  -->>  null and empty
        if (getProtocols() == null
                && (getProvider() == null || getProvider().getProtocols() == null)){

            Map<String, ProtocolConfig> protocolConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
            if (!CollectionUtils.isEmpty(protocolConfigMap)){
                List<ProtocolConfig> protocolConfigs = new ArrayList<>();
                for (ProtocolConfig config : protocolConfigMap.values()) {
                    if (config.getIsDefault() == null || config.getIsDefault().booleanValue()){
                        protocolConfigs.add(config);
                    }
                }
                if (!CollectionUtils.isEmpty(protocolConfigs)){
                    setProtocols(protocolConfigs);
                }
            }
        }

        // Path -->  null.
        if (StringUtils.isEmpty(getPath())){
            if (!StringUtils.isEmpty(beanName)
                    && !StringUtils.isEmpty(getInterface())
                    && beanName.startsWith(getInterface())){
                setPath(beanName);
            }
        }

        if (!isDelay()){
            export();
        }
    }
}
