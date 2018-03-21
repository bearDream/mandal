package cn.ching.mandal.common.serialize;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface ObjectInput extends DataInput {

    /**
     * read object.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    Object readObject() throws IOException, ClassNotFoundException;

    /**
     * read Object.
     * @param clazz
     * @param <T>
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    <T> T readObject(Class<T> clazz) throws IOException, ClassNotFoundException;

    /**
     * read object.
     * @param clazz
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    <T> T readObject(Class<T> clazz, Type type) throws IOException, ClassNotFoundException;
}
