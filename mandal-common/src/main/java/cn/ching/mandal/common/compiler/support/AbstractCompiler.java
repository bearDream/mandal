package cn.ching.mandal.common.compiler.support;

import cn.ching.mandal.common.compiler.Compiler;
import cn.ching.mandal.common.utils.ClassHelper;
import cn.ching.mandal.common.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2018/1/9
 * <p>
 *     First, match code if it has package and class.
 *     second, use {@link Class#forName(String, boolean, ClassLoader)} load class, if this class exist;
 *      if this class not exits, invoke {@link AbstractCompiler#doCompile(String, String)} get class.
 * </p>
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractCompiler implements Compiler {

    private static final Pattern PACKAGE_PATTER = Pattern.compile("package\\s+([$_a-zA-Z][$_a-zA-Z0-9\\.]*);");

    private static final Pattern CLASS_PATTER = Pattern.compile("class\\s+([$_a-zA-Z][$_a-zA-Z0-9]*)\\s+");

    @Override
    public Class<?> compiler(String code, ClassLoader classLoader) {
        code = code.trim();
        Matcher matcher = PACKAGE_PATTER.matcher(code);
        String packageName;
        if (matcher.find()){
            packageName = matcher.group(1);
        }else {
            packageName = "";
        }

        matcher = CLASS_PATTER.matcher(code);
        String cls;
        if (matcher.find()){
            cls = matcher.group(1);
        }else {
            throw new IllegalArgumentException("no such class name in " + code);
        }
        String className = StringUtils.isBlank(packageName) ? cls : packageName + "." + cls;
        try {
            return Class.forName(className, true, ClassHelper.getCallerClassLoader(getClass()));
        } catch (ClassNotFoundException e){
            if (!code.endsWith("}")){
                throw new IllegalArgumentException("not as } end class in "+code);
            }

            try {
                return doCompile(className, code);
            } catch (RuntimeException rt){
                throw rt;
            }catch (Throwable tx) {
                throw new IllegalStateException("failed compile class code, cause by: " + tx.getMessage() + "  class:" + className + " code:" + code);
            }
        }
    }

    protected abstract Class<?> doCompile(String name, String source) throws Throwable;

}
