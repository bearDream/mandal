package cn.ching.mandal.demo.controller;

import cn.ching.mandal.config.annoatation.Reference;
import cn.ching.mandal.demo.api.RpcIndexService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: laxzhang
 * @email: laxzhang@outlook.com
 * @description: controller
 **/
@RestController
public class MandalBootController {

    @Reference(version = "0.0.2",
            application = "${mandal.application.id}",
            registry = "${mandal.registry.id}")
    private RpcIndexService indexService;

    @RequestMapping(value = "index")
    public String index(@RequestParam String msg){
        return indexService.index(msg);
    }
}
