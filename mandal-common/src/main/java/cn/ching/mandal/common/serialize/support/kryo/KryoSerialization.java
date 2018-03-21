package cn.ching.mandal.common.serialize.support.kryo;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.serialize.ObjectInput;
import cn.ching.mandal.common.serialize.ObjectOutput;
import cn.ching.mandal.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class KryoSerialization implements Serialization{
    @Override
    public byte getContentTypeId() {
        return 0;
    }

    @Override
    public String getContentType() {
        return "x-application/kryo";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new KryoObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new KryoObjectInput(input);
    }
}
