package cn.ching.mandal.config.spring;

import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.config.*;
import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.config.spring.extension.SpringExtensionFactory;
import cn.ching.mandal.config.support.Parameter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;

/**
 * 2018/4/1
 * config Service Consumer Bean as Spring Bean.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {

    private static final long serialVersionUID = -540125766808681240L;

    private transient ApplicationContext applicationContext;

    public ReferenceBean(){
        super();
    }

    public ReferenceBean(Reference reference){
        super(reference);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    @Override
    public Object getObject() throws Exception {
        return get();
    }

    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Parameter(exclude = true)
    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // Consumer is null
        if (getConsumer() == null){
            Map<String, ConsumerConfig> consumerConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class, false, false);
            if (!CollectionUtils.isEmpty(consumerConfigMap)){
                ConsumerConfig consumerConfig = null;
                // check whether consumer config more than 1.
                if (consumerConfigMap.values().size() > 1){
                    throw new IllegalStateException("Duplicate consumer config. " + consumerConfig + " and " + consumerConfigMap.values().iterator().next());
                }
                consumerConfig = consumerConfigMap.values().iterator().next();
                if (!Objects.isNull(consumerConfig)){
                    setConsumer(consumerConfig);
                }
            }
        }

        // Application && (Consumer  ||  Consumer.Application) is null
        if (getApplication() == null
                && (getConsumer() == null || getConsumer().getApplication() == null)){
            Map<String, ApplicationConfig> applicationConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
            if (!CollectionUtils.isEmpty(applicationConfigMap)){
                ApplicationConfig applicationConfig = null;
                if (applicationConfigMap.size() > 1){
                    throw new IllegalStateException("Duplicate consumer config. " + applicationConfig + " and " + applicationConfigMap.values().iterator().next());
                }
                applicationConfig = applicationConfigMap.values().iterator().next();
                if (!Objects.isNull(applicationConfig)){
                    setApplication(applicationConfig);
                }
            }
        }

        // Module && (Consumer  ||  Consumer.Module) is null.
        if (getModule() == null
                && (getConsumer() == null || getConsumer().getModule() == null)){
            Map<String, ModuleConfig> moduleConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class, false, false);
            if (!CollectionUtils.isEmpty(moduleConfigMap)){
                ModuleConfig moduleConfig = null;
                if (moduleConfigMap.size() > 1){
                    throw new IllegalStateException("Duplicate module config. " + moduleConfig + " and " + moduleConfigMap.values().iterator().next());
                }
                moduleConfig = moduleConfigMap.values().iterator().next();
                if (!Objects.isNull(moduleConfig)){
                    setModule(moduleConfig);
                }
            }
        }

        // Registries  &&  (Consumer  ||  Consumer.Registries) && (Application  ||  Application.Registries)  is null
        if (getRegistries() == null
                && (getConsumer() == null || getConsumer().getRegistries() == null)
                && (getApplication() == null || getApplication().getRegistries() == null)){
            Map<String, RegistryConfig> registryConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (!CollectionUtils.isEmpty(registryConfigMap)){
                List<RegistryConfig> registryConfigs = new ArrayList<>();

                registryConfigMap.values()
                        .stream()
                        .filter(config -> config.getIsDefault() == null || config.getIsDefault().booleanValue())
                        .forEach(config -> {
                            registryConfigs.add(config);
                        });
                if (!CollectionUtils.isEmpty(registryConfigs)){
                    setRegistries(registryConfigs);
                }
            }
        }

        // Monitor &&  (Consumer  ||  Consumer.Monitor)  &&  (Application  ||  Application.Monitor)  is null
        if (getMonitor() == null
                && (getConsumer() == null || getConsumer().getMonitor() == null)
                && (getApplication() == null || getApplication().getMonitor() == null)){
            Map<String, MonitorConfig> monitorConfigMap = Objects.isNull(applicationContext) ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class, false, false);
            if (!CollectionUtils.isEmpty(monitorConfigMap)){
                MonitorConfig monitorConfig = null;
                if (monitorConfigMap.values().size() > 1){
                    throw new IllegalStateException("Duplicate monitor config. " + monitorConfig + " and " + monitorConfigMap.values().iterator().next());
                }
                monitorConfig = monitorConfigMap.values().iterator().next();
                if (!Objects.isNull(monitorConfig)){
                    setMonitor(monitorConfig);
                }
            }
        }

        Boolean b = getInit();
        if (Objects.isNull(b) && !Objects.isNull(getConsumer())){
            b = getConsumer().getInit();
        }
        if (!Objects.isNull(b) && b.booleanValue()) {
            getObject();
        }
    }
}
