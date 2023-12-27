package qingzhou.ssh.impl;

import qingzhou.ssh.SSHService;
import qingzhou.ssh.SshResult;

import java.io.IOException;
import java.util.Map;

// todo 对接开源版本的实现，参考 qingzhou-logger 将需要的jar打包进一个jar里
public class SSHImpl implements SSHService {
    @Override
    public SSHService start(Map<String, String> config) {
        return null;
    }

    @Override
    public SshResult execCmd(String cmd) throws Exception {
        return null;
    }

    @Override
    public SshResult execShell(String cmd) throws Exception {
        return null;
    }

    @Override
    public String uploadFile(String src, String dist) throws IOException {
        return null;
    }

    @Override
    public String downLoadFile(String src, String dist) throws IOException {
        return null;
    }

    @Override
    public void close() {

    }
}
