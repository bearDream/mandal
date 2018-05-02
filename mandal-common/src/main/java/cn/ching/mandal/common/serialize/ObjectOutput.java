package cn.ching.mandal.common.serialize;

import java.io.IOException;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface ObjectOutput extends DataOutput{

    /**
     * write object.
     * @param obj
     * @throws IOException
     */
    void writeObject(Object obj) throws IOException;
}
