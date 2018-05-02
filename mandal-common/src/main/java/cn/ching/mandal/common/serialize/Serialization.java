package cn.ching.mandal.common.serialize;

import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.Adaptive;
import cn.ching.mandal.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@SPI("kryo")
public interface Serialization {

    /**
     * get content type id.
     * @return
     */
    byte getContentTypeId();

    /**
     * get content type.
     * @return
     */
    String getContentType();

    /**
     * create serialize.
     * @param url
     * @param output
     * @return
     * @throws IOException
     */
    @Adaptive
    ObjectOutput serialize(URL url, OutputStream output) throws IOException;

    /**
     * create deserializable.
     * @param url
     * @param input
     * @return
     * @throws IOException
     */
    @Adaptive
    ObjectInput deserialize(URL url, InputStream input) throws IOException;
}
