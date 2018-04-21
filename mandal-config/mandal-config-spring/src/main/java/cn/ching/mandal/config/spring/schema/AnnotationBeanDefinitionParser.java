package cn.ching.mandal.config.spring.schema;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.config.spring.beans.factory.annotation.ReferenceAnnotaionBeanPostProcessor;
import cn.ching.mandal.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;
import cn.ching.mandal.config.spring.utils.BeanRegistrator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * 2018/4/20
 *
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 *
 * @see cn.ching.mandal.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor
 * @see cn.ching.mandal.config.spring.beans.factory.annotation.ReferenceAnnotaionBeanPostProcessor
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class AnnotationBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    /**
     * parser XML config file.
     * <prev>
     * like: <mandal:annotation package=""/>
     * </prev>
     * @param element
     * @param builder
     */
    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        String pkgToScan = element.getAttribute("package");

        String[] packageToScan = StringUtils.trimArrayElements(Constants.COMMA_SPLIT_PATTERN.split(pkgToScan));

        builder.addConstructorArgValue(pkgToScan);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        registerReferenceAnnotationBeanPostProcessor(parserContext.getRegistry());
    }

    /**
     * register {@link ReferenceAnnotaionBeanPostProcessor} reference service
     * @param registry
     */
    private void registerReferenceAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {

        BeanRegistrator.registerInfrastructureBean(registry, ReferenceAnnotaionBeanPostProcessor.BEAN_NAME, ReferenceAnnotaionBeanPostProcessor.class);
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return ServiceAnnotationBeanPostProcessor.class;
    }
}
