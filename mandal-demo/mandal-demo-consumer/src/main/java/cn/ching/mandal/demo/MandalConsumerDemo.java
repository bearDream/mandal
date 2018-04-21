package cn.ching.mandal.demo;

import cn.ching.mandal.demo.api.RpcIndexService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Objects;

/**
 * 2018/4/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MandalConsumerDemo {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/mandal-consumer-config.xml"});
        context.start();
        Object i = context.getBean("indexService");
        System.out.println(i);
        RpcIndexService indexService = (RpcIndexService) context.getBean("indexService");

        while (true){
            try {
                Thread.sleep(500);
                String index = indexService.index();
                System.out.println(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
