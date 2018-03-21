package cn.ching.mandal.common.serialize.support.kryo.utils;

import com.esotericsoftware.kryo.Kryo;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ThreadLocalKryoFactory extends AbstractKryoFactory {

    private final ThreadLocal<Kryo> holder = ThreadLocal.withInitial(() -> create());

    @Override
    public void returnKryo(Kryo kryo) {}

    @Override
    public Kryo getKryo() {
        return holder.get();
    }
}
