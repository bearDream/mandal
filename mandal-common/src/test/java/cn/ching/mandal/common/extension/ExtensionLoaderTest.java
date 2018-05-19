package cn.ching.mandal.common.extension;


import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.compiler.Compiler;
import cn.ching.mandal.common.threadpool.ThreadPool;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class ExtensionLoaderTest{


    @Test
    public void TestGetExtensionLoader(){
        ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Compiler.class);
        System.out.println(loader);
    }

    @Test
    public void TestGetExtensionLoaderAdaptor(){
        ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Compiler.class);
        StringBuilder sb = new StringBuilder(ExtensionLoaderTest.class.getPackage() + ";");
        sb.append("\n import " + ExtensionLoader.class.getName() + ";");
        sb.append("\n public class Test$Adaptive {");
        sb.append("\n public String outprint(int i){");
        sb.append("\n return(\"haha\" + i);");
        sb.append("\n }");
        sb.append("\n }");
        Compiler compiler = (Compiler) loader.getExtension("jdk");
        Class clz = compiler.compiler(sb.toString(), getClass().getClassLoader());
        Method[] methods = clz.getMethods();
        for (Method method : methods) {
            try {
                if (method.getName() == "outprint"){
                    String res = (String) method.invoke(clz.newInstance(), 123);
                    System.out.println(res);
                    Assert.assertEquals("haha123", res);
                }
            }catch (Exception e){
                System.err.println(e.getMessage());
            }
        }
    }

    @Test
    public void TestCompiler() throws Exception {
        try {
            Compiler compiler = ExtensionLoader.getExtensionLoader(Compiler.class).getExtension("jdk");

            StringBuilder sb = new StringBuilder(ExtensionLoaderTest.class.getPackage() + ";");
            sb.append("\n import " + ExtensionLoader.class.getName() + ";");
            sb.append("\n public class Test$Adaptive {");
            sb.append("\n public String outprint(int i){");
            sb.append("\n return(\"haha\" + i);");
            sb.append("\n }");
            sb.append("\n }");
            Class<?> cls = compiler.compiler(sb.toString(), getClass().getClassLoader());
            System.out.println(cls.getName());
            Method[] methods = cls.getMethods();
            for (Method method : methods) {
                try {
                    if (method.getName() == "outprint"){
                        String res = (String) method.invoke(cls.newInstance(), 123);
                        Assert.assertEquals("haha123", res);
                    }
                }catch (Exception e){
                    System.err.println(e.getMessage());
                }
            }
        }catch (IllegalStateException e){
            System.err.println(e);
        }
    }
}