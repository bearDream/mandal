package cn.ching.mandal.common.extension;

import java.lang.annotation.*;

/**
 * 2018/1/5
 * Activate. This annotation is useful for automatically activate certain extensions with the given criteria,
 * for examples: <code>@Activate</code> can be used to load certain <code>Filter</code> extension when there are
 * multiple implementations.
 * <ol>
 * <li>{@link Activate#group()} specifies group criteria. Framework SPI defines the valid group values.
 * <li>{@link Activate#value()} specifies parameter key in {@link URL} criteria.
 * </ol>
 * SPI provider can call {@link ExtensionLoader#getActivateExtension(URL, String, String)} to find out all activated
 * extensions with the given criteria.
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate {

    /**
     * Activate the current extension when one of the group matches. the group passed into
     * {@link ExtensionLoader}
     * @return group name to match
     */
    String[] group() default {};

}
