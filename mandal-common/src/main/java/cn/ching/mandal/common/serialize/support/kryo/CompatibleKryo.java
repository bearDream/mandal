package cn.ching.mandal.common.serialize.support.kryo;

import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.serialize.support.kryo.utils.ReflectionUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import java.util.Objects;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class CompatibleKryo extends Kryo{

    private static final Logger logger = LoggerFactory.getLogger(CompatibleKryo.class);

    @Override
    public Serializer getDefaultSerializer(Class type) {

        if (Objects.isNull(type)){
            throw new IllegalArgumentException("type can not be null.");
        }

        if (!type.isArray() && !type.isEnum() && !ReflectionUtils.checkZeroArgConstructor(type)){
            if (logger.isWarnEnabled()){
                logger.warn(type + " has no args constructor and this will affect the serialization performance!");
            }
            return new JavaSerializer();
        }

        return super.getDefaultSerializer(type);
    }
}
