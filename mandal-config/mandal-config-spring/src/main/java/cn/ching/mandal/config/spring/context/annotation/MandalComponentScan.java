package cn.ching.mandal.config.spring.context.annotation;

import cn.ching.mandal.config.annoatation.*;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 2018/4/7
 *
 * Scans classpath for annotation components that will be auto registered as spring beans,
 * Mandal-provider {@link Service} and {@link Reference}.
 * @see Service
 * @see Reference
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MandalComponentScanRegister.class)
public @interface MandalComponentScan {

    /**
     * Alias for the {@link #basePackage()}.
     * like that: {@code @{MandalComponentScan("com.my.package")}} instead of {@code @{MandalComponentScan(basePackage="com.my.package")}}
     * @return the base packages to scan.
     */
    String[] value() default {};

    /**
     * Base package to scan for annotated @Service classes. {@link #value()} is an alias for
     * (and mutually exclusive with) this attribute.
     *
     * @return the base packages to scan.
     */
    String[] basePackage() default {};

    /**
     * Type-safe alternative to {@link #basePackage()} for specifying the packages to scan for
     * annotated @Service classes.
     * @return classes for base package to scan.
     */
    Class<?>[] basePackageClasses() default {};
}
