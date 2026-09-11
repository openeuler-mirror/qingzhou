#!/bin/sh

# 为 instance 生成自签 TLS 密钥库，并用随机口令回写 qingzhou.properties。
# 用法：bin/gen-keystore.sh [instance]
# 私钥不随包分发，每个 instance 必须生成自己的密钥库。

export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

if [ -n "$1" ]; then
    runInstance=$1
else
    runInstance="default"
fi

confDir="${qingzhou_home}/instances/${runInstance}/conf"
props="${confDir}/qingzhou.properties"
keystore="${confDir}/keystore.p12"

if ! [ -d "${confDir}" ]; then
    echo "Instance does not exist: ${runInstance}"
    exit 1
fi

password=$(head -c 24 /dev/urandom | base64 | tr -d '/+=\n')
if [ -z "${password}" ]; then
    echo "failed to generate keystore password"
    exit 1
fi

rm -f "${keystore}"

keytool -genkeypair -alias qingzhou -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
    -validity 3650 -storetype PKCS12 -keystore "${keystore}" \
    -storepass "${password}" -keypass "${password}" \
    -dname "CN=qingzhou" -ext "SAN=dns:localhost,ip:127.0.0.1" || exit 1

chmod 600 "${keystore}"

# 回写口令：存在则替换，不存在则追加
sed "s|^qingzhou-http-server.ssl_keystore_password=.*|qingzhou-http-server.ssl_keystore_password=${password}|" "${props}" > "${props}.tmp"
if grep -q '^qingzhou-http-server.ssl_keystore_password=' "${props}"; then
    mv "${props}.tmp" "${props}"
else
    rm -f "${props}.tmp"
    echo "qingzhou-http-server.ssl_keystore_password=${password}" >> "${props}"
fi

echo "keystore generated: ${keystore}"
