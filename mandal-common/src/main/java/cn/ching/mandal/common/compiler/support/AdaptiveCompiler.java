package cn.ching.mandal.common.compiler.support;

import cn.ching.mandal.common.compiler.Compiler;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.utils.StringUtils;

/**
 * 2018/1/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Adaptive
public class AdaptiveCompiler implements Compiler{

    private static volatile String DEFAULT_COMPILER;

    public static void setDefaultCompiler(String compiler){
        DEFAULT_COMPILER = compiler;
    }

    @Override
    public Class<?> compiler(String code, ClassLoader classLoader) {
        Compiler compiler;
        ExtensionLoader<Compiler> loader = ExtensionLoader.getExtensionLoader(Compiler.class);
        String name = DEFAULT_COMPILER;
        if (StringUtils.isBlank(name)){
            compiler = loader.getDefaultExtension();
        }else {
            compiler = loader.getExtension(name);
        }

        return compiler.compiler(code, classLoader);
    }
}
