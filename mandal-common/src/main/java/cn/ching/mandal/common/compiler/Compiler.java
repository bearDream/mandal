package cn.ching.mandal.common.compiler;

import cn.ching.mandal.common.extension.SPI;

/**
 * 2018/1/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("jdk")
public interface Compiler {

    /**
     * compile code
     * @param code   java source code
     * @param classLoader
     * @return compiled class
     */
    Class<?> compiler(String code, ClassLoader classLoader);
}
