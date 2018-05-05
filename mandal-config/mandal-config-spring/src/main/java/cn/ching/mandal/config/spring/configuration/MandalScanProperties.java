package cn.ching.mandal.config.spring.configuration;

import cn.ching.mandal.config.spring.context.annotation.EnableMandal;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

import static cn.ching.mandal.config.spring.utils.MandalUtils.Mandal_SCAN_PREFIX;

/**
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: config properties
 **/
@ConfigurationProperties(prefix = Mandal_SCAN_PREFIX)
public class MandalScanProperties {

    /**
     * @see EnableMandal#scanBasePackages()
     */
    private Set<String> basePackage = new LinkedHashSet<>();

    public Set<String> getBasePackage(){
        return basePackage;
    }

    public void setBasePackage(Set<String> basePackage) {
        this.basePackage = basePackage;
    }
}
