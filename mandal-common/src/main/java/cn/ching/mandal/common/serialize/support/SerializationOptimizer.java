package cn.ching.mandal.common.serialize.support;

import java.util.Collection;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface SerializationOptimizer {

    Collection<Class> getSerializableClasses();
}
