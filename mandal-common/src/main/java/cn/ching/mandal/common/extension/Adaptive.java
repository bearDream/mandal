package cn.ching.mandal.common.extension;

import java.lang.annotation.*;

/**
 * 2018/1/5
 * <p>
 * every SPI Component has a Interface and adaptive class and implication class.
 * 1、if no adaptive class, then the interface method need add {@link Adaptive} and at least method have one {@link cn.ching.mandal.common.URL} type parameter,
 *  both of the above are satisfied, the {@link ExtensionLoader} can auto gerate adaptive class code and compiler it.
 * 2、if no adaptive class, and interface method no {@link Adaptive} or interface method no as {@link cn.ching.mandal.common.URL} as parameter type, then throw error.
 * </p>
 * @see {@link SPI}
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {

    /**
     * Decide which target extension to be injected. The name of the target extension is decided by the parameter passed
     * in the URL, and the parameter names are given by this method.
     * <p>
     * If the specified parameters are not found from {@link cn.ching.mandal.common.URL}, then the default extension will be used for
     * dependency injection (specified in its interface's {@link SPI}).
     * <p>
     * For examples, given <code>String[] {"key1", "key2"}</code>:
     * <ol>
     * <li>find parameter 'key1' in URL, use its value as the extension's name</li>
     * <li>try 'key2' for extension's name if 'key1' is not found (or its value is empty) in URL</li>
     * <li>use default extension if 'key2' doesn't appear either</li>
     * <li>otherwise, throw {@link IllegalStateException}</li>
     * </ol>
     * If default extension's name is not give on interface's {@link SPI}, then a name is generated from interface's
     * class name with the rule: divide classname from capital char into several parts, and separate the parts with
     * dot '.', for example: for {@code com.alibaba.dubbo.xxx.YyyInvokerWrapper}, its default name is
     * <code>String[] {"yyy.invoker.wrapper"}</code>. This name will be used to search for parameter from URL.
     *
     * @return parameter key names in URL
     */
    String[] value() default {};
}
