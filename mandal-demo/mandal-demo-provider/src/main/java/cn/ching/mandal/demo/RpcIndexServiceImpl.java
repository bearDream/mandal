package cn.ching.mandal.demo;

import cn.ching.mandal.demo.api.RpcIndexService;

/**
 * 2018/4/7
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RpcIndexServiceImpl implements RpcIndexService {

    @Override
    public String index(String msg) {
        return "Hello! Im mandal. Nice to meet you. what are you want to say? " + msg;
    }
}
