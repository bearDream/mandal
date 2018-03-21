package cn.ching.mandal.common.serialize.support.kryo;

import cn.ching.mandal.common.serialize.Cleanable;
import cn.ching.mandal.common.serialize.ObjectOutput;
import cn.ching.mandal.common.serialize.support.kryo.utils.KryoUtil;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * 2018/3/21
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class KryoObjectOutput implements ObjectOutput, Cleanable{

    private Output output;
    private Kryo kryo;

    public KryoObjectOutput(OutputStream out){
        this.output = new Output(out);
        this.kryo = KryoUtil.get();
    }

    @Override
    public void cleanup() {
        KryoUtil.release(kryo);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        kryo.writeClassAndObject(output, obj);
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        output.writeByte(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        output.writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        output.writeString(v);
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        if (Objects.isNull(v)){
            output.writeInt(-1);
        }else {
            writeBytes(v, 0, v.length);
        }
    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        if (Objects.isNull(v)){
            output.writeInt(-1);
        }else {
            output.writeInt(len);
            output.write(v, off, len);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        output.flush();
    }
}
