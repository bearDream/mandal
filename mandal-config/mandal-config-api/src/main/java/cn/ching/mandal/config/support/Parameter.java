package cn.ching.mandal.config.support;

import java.lang.annotation.*;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Parameter {

    String key() default "";

    // this method must have value.
    boolean required() default false;

    boolean exclude() default false;

    boolean escaped() default false;

    boolean attribute() default false;

    boolean append() default false;
}
