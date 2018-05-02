package cn.ching.mandal.common.serialize.support.kryo.utils;

import cn.ching.mandal.common.serialize.support.SerializableClassRegistry;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import de.javakaffee.kryoserializers.*;

import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractKryoFactory implements KryoFactory{

    private final Set<Class> registrations = new LinkedHashSet<>();

    private boolean registrationRequired;

    private volatile boolean kryoCreated;

    public AbstractKryoFactory(){}

    public void registerClass(Class clazz){

        if (kryoCreated){
            throw new IllegalStateException("Kryo has been created. You can't continue created Kryo.");
        }
        registrations.add(clazz);
    }

    @Override
    public Kryo create() {
        if (!kryoCreated){
            kryoCreated = true;
        }

        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(registrationRequired);

        kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
        kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(InvocationHandler.class, new JdkProxySerializer());
        kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
        kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
        kryo.register(Pattern.class, new RegexSerializer());
        kryo.register(BitSet.class, new BitSetSerializer());
        kryo.register(URI.class, new URISerializer());
        kryo.register(UUID.class, new UUIDSerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);

        // basic serialization.
        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedList.class);
        kryo.register(HashSet.class);
        kryo.register(TreeSet.class);
        kryo.register(Hashtable.class);
        kryo.register(Date.class);
        kryo.register(Calendar.class);
        kryo.register(ConcurrentMap.class);
        kryo.register(SimpleDateFormat.class);
        kryo.register(GregorianCalendar.class);
        kryo.register(Vector.class);
        kryo.register(BitSet.class);
        kryo.register(StringBuilder.class);
        kryo.register(StringBuffer.class);
        kryo.register(Object.class);
        kryo.register(Object[].class);
        kryo.register(String[].class);
        kryo.register(byte[].class);
        kryo.register(char[].class);
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(double[].class);

        registrations.forEach(clazz -> kryo.register(clazz));
        SerializableClassRegistry.getRegisteredClasses().forEach(clazz -> kryo.register(clazz));

        return kryo;
    }

    public void setRegistrationRequired(boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
    }

    public abstract void returnKryo(Kryo kryo);

    public abstract Kryo getKryo();
}
