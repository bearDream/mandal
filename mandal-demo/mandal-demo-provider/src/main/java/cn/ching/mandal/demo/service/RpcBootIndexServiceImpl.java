package cn.ching.mandal.demo.service;

import cn.ching.mandal.config.annoatation.Service;
import cn.ching.mandal.demo.api.RpcIndexService;

/**
 * 2018/4/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Service(version = "0.0.2", application = "${mandal.application.id}", protocol = "${mandal.protocol.id}", registry = "${mandal.registry.id}")
public class RpcBootIndexServiceImpl implements RpcIndexService {

    @Override

    public String index(String msg) {
        System.out.println("consumer invoke me!");
        return "Hello! Im mandal. Nice to meet you. what are you want to say? " + msg;
    }
}
