package cn.ching.mandal.remoting.zookeeper;

import java.util.List;

/**
 * 2018/1/26
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface ChildListener {

    void childChanged(String path, List<String> children);
}
