package cn.ching.mandal.demo;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * 2018/4/20
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class Provider {

    public static void main(String[] args) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/mandal-provider-config.xml"});
        context.start();

        System.out.println("mandal provider demo is started..");
        System.in.read();
    }
}
