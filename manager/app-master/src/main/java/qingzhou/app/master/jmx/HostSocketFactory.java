package qingzhou.app.master.jmx;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;

class HostSocketFactory extends RMISocketFactory {

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return RMISocketFactory.getDefaultSocketFactory().createSocket(host, port);
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        String hostname = System.getProperty("java.rmi.server.hostname");
        if (hostname == null) {
            return new ServerSocket(port);
        } else {
            try {
                InetAddress address = InetAddress.getByName(hostname);
                return new ServerSocket(port, 0, address);
            } catch (UnknownHostException var3) {
                return new ServerSocket(port);
            }
        }
    }
}
