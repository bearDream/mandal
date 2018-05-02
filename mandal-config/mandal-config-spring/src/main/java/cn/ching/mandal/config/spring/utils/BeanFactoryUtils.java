package cn.ching.mandal.config.spring.utils;

import cn.ching.mandal.common.utils.StringUtils;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * 2018/3/8
 * BeanFactoryUtils
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class BeanFactoryUtils {

    /**
     * Get optional bean for {@link ListableBeanFactory}
     * @param beanFactory
     * @param beanName
     * @param beanType
     * @param <T>
     * @return
     */
    public static <T> T getOptionalBean(ListableBeanFactory beanFactory, String beanName, Class<T> beanType){

        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);
        if (!StringUtils.isContains(allBeanNames, beanName)){
            return null;
        }

        Map<String, T> beansOfType = beansOfTypeIncludingAncestors(beanFactory, beanType);
        return beansOfType.get(beanName);
    }

    /**
     * Get bean by name matched for {@link ListableBeanFactory}
     * @param beanFactory
     * @param beanNames
     * @param beanType
     * @param <T>
     * @return
     */
    public static <T> List<T> getBeans(ListableBeanFactory beanFactory, String[] beanNames, Class<T> beanType){

        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);

        List<T> beans = new ArrayList<>(beanNames.length);

        for (String beanName : beanNames) {
            if (StringUtils.isContains(allBeanNames, beanName)){
                beans.add(beanFactory.getBean(beanName, beanType));
            }
        }

        return beans;
    }



}
