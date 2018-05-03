package cn.ching.mandal.demo.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: provider demo
 **/
@SpringBootApplication(scanBasePackages = "cn.ching.mandal.demo.controller")
public class MandalConsumerDemo {

    public static void main(String[] args) {
        SpringApplication.run(MandalConsumerDemo.class, args);
    }
}
