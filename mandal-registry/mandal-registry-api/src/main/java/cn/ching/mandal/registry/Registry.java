package cn.ching.mandal.registry;

import cn.ching.mandal.common.Node;

/**
 * 2018/1/15
 * registry only support provider registry and consumer subscribe.
 * The way of service invoke by consumer directly invoke provider.
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Registry extends Node, RegistryService{
}
