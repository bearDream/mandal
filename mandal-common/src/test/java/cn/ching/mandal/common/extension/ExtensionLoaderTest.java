package cn.ching.mandal.common.extension;


import cn.ching.mandal.common.compiler.Compiler;
import cn.ching.mandal.common.extension.filter.Filter;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionLoaderTest {

    @Test
    public void getExtensionLoader() throws Exception {
        try {
            Filter filter = ExtensionLoader.getExtensionLoader(Filter.class).getAdaptiveExtension();
            System.out.println(filter);
            Assert.fail();
        }catch (IllegalStateException e){
            System.out.println(e);
        }
    }

}