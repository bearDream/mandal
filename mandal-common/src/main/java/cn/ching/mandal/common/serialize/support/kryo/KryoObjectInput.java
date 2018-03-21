package cn.ching.mandal.common.serialize.support.kryo;

import cn.ching.mandal.common.serialize.Cleanable;
import cn.ching.mandal.common.serialize.ObjectInput;
import cn.ching.mandal.common.serialize.support.kryo.utils.KryoUtil;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class KryoObjectInput implements ObjectInput, Cleanable {

    private Kryo kryo;
    private Input input;

    public KryoObjectInput(InputStream in){
        in = new Input(in);
        this.kryo = KryoUtil.get();
    }

    @Override
    public void cleanup() {
        KryoUtil.release(kryo);
        kryo = null;
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        try {
            return kryo.readClassAndObject(input);
        }catch (KryoException e){
            throw new IOException(e);
        }
    }

    @Override
    public <T> T readObject(Class<T> clazz) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    @Override
    public <T> T readObject(Class<T> clazz, Type type) throws IOException, ClassNotFoundException {
        return readObject(clazz);
    }

    @Override
    public boolean readBool() throws IOException {
        try {
            return input.readBoolean();
        }catch (KryoException e){
            throw new IOException(e);
        }
    }

    @Override
    public byte readByte() throws IOException {
        try {
            return input.readByte();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public short readShort() throws IOException {
        try {
            return input.readShort();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public int readInt() throws IOException {
        try {
            return input.readInt();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public long readLong() throws IOException {
        try {
            return input.readLong();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public float readFloat() throws IOException {
        try {
            return input.readFloat();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public double readDouble() throws IOException {
        try {
            return input.readDouble();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public String readUTF() throws IOException {
        try {
            return input.readString();
        }catch (KryoException e){
            throw new IOException(e);
        }    }

    @Override
    public byte[] readBytes() throws IOException {
        try {
            int len = input.readInt();
            if (len < 0){
                return null;
            }else if (len == 0){
                return new byte[]{};
            }else {
                return input.readBytes(len);
            }
        }catch (KryoException e){
            throw new IOException(e);
        }
    }
}
