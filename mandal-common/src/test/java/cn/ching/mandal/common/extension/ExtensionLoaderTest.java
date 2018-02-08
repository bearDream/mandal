package cn.ching.mandal.common.extension;


import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.compiler.Compiler;
import cn.ching.mandal.common.extension.filter.Filter;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

public class ExtensionLoaderTest{


    @Test
    public static void getExtensionLoader() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(Compiler.class).getExtension("jdk");
        }catch (IllegalStateException e){
            System.err.println(e);
        }
    }

}