package qingzhou.serializer.impl.java;

import qingzhou.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JavaSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.close();
        return bos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws Exception {
        ObjectInputStream ois = new SafeObjectInputStream(new ByteArrayInputStream(bytes));
        return (T) ois.readObject();
    }
}
