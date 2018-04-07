package cn.ching.mandal.config.spring.context.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 2018/4/7
 *
 * Enable Mandal components as Spring beans.
 * equals {@link EnableMandalConfig} and {@link MandalComponentScan} combinations.
 * note: spring framework must higher than 4.2.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnableMandalConfig
@MandalComponentScan
public @interface EnableMandal {

    /**
     * Base packages to scan for annotated @Service classes.
     * @return
     */
    @AliasFor(annotation = MandalComponentScan.class, attribute = "basePackages")
    String[] scanBasePackages() default {};

    /**
     * like {@link MandalComponentScan#basePackageClasses()}
     * @return
     */
    @AliasFor(annotation = MandalComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] scanBasePackageClasses() default {};

    /**
     * like {@link cn.ching.mandal.config.AbstractConfig} bindings multiple spring beans.
     * @return
     * @see EnableMandalConfig#multiple()
     */
    @AliasFor(annotation = EnableMandalConfig.class, attribute = "multiple")
    boolean multipleConfig() default false;

}
