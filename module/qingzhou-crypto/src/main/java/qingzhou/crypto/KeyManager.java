package qingzhou.crypto;

import qingzhou.framework.pattern.Callback;

import java.io.File;

public interface KeyManager {
    String getKey(File keyFile, String keyName) throws Exception;

    String getKeyOrElseInit(File keyFile, String keyName, Callback<Void, String> initKey) throws Exception;

    void writeKey(File keyFile, String keyName, String keyVal) throws Exception;

    String getKeyPair(File keyFile, String keyName, String publicKeyName, String privateKeyName) throws Exception;

    String getKeyPairOrElseInit(File keyFile, String keyName, String publicKeyName, String privateKeyName, Callback<Void, String[]> initKey) throws Exception;
}
