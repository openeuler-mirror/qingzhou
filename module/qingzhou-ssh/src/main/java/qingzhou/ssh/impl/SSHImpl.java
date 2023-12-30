package qingzhou.ssh.impl;

import qingzhou.ssh.SSHService;
import qingzhou.ssh.SSHSession;

import java.util.Map;

// todo 对接开源版本的实现，参考 qingzhou-logger 将需要的jar打包进一个jar里
public class SSHImpl implements SSHService {

    @Override
    public SSHSession buildSession(Map<String, String> config) {
        return null;
    }
}
