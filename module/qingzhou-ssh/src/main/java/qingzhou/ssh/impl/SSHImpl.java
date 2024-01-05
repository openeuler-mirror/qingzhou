package qingzhou.ssh.impl;

import qingzhou.ssh.SSHService;
import qingzhou.ssh.SSHClient;

import java.io.IOException;
import java.util.Map;

// todo 对接开源版本的实现，参考 qingzhou-logger 将需要的jar打包进一个jar里
public class SSHImpl implements SSHService {

    @Override
    public SSHClient buildClient(Map<String, String> config) throws IOException {
        return new SSHClientImpl().start(config);
    }
}
