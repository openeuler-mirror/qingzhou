package qingzhou.remote.net.bio;

import qingzhou.remote.net.Channel;
import qingzhou.remote.net.Codec;
import qingzhou.remote.net.Handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class BIOChannel implements Channel {

    private final Socket socket;
    private final Handler handler;
    private Codec codec;

    public BIOChannel(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public synchronized Object read() throws IOException {
        byte[] message = read(this);
        Object obj = codec.decode(message, this);
        return handler.received(obj, this);
    }

    @Override
    public synchronized void write(Object message) throws IOException {
        // todo 优化用 责任链实现 压缩 加密 编码 日志记录等功能
        Object req = handler.sent(message, this);
        Object encode = codec.encode(req, this);
        write((byte[]) encode);
    }

    private void write(byte[] bytes) throws IOException {
        Socket socket = getSocket();
        OutputStream outputStream = socket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    private byte[] read(Channel channel) throws IOException {
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return bytes;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    public Codec getCodec() {
        return codec;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }
}
