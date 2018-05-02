package cn.ching.mandal.config.spring.schema;

import cn.ching.mandal.common.Version;
import cn.ching.mandal.config.*;
import cn.ching.mandal.config.spring.ReferenceBean;
import cn.ching.mandal.config.spring.ServiceBean;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 2018/4/20
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalNamespaceHandler extends NamespaceHandlerSupport{

    static {
        // avoid conflict.
        Version.checkDuplicate(MandalNamespaceHandler.class);
    }

    @Override
    public void init() {
        registerBeanDefinitionParser("application", new MandalBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new MandalBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new MandalBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("monitor", new MandalBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new MandalBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new MandalBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new MandalBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new MandalBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser("reference", new MandalBeanDefinitionParser(ReferenceBean.class, true));
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }

}
