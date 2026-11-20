#!/usr/bin/env bash
# 创建轻舟应用工程骨架：生成 pom.xml，并把同版本平台依赖放入工程内本地仓库。
set -euo pipefail

usage() {
    echo "用法: $0 <应用标识> <轻舟版本> <产品库目录> [工程根目录]" >&2
    echo "  产品库目录: 产品包内的 lib/version<版本>，含 modules/qingzhou-api.jar、components/qingzhou-json.jar" >&2
    exit 1
}

[ $# -ge 3 ] || usage
app=$1
version=$2
library=$3
root=${4:-$(pwd)}

[[ $app =~ ^[a-z][a-z0-9-]{0,63}$ ]] || {
    echo "应用标识须为小写字母开头、仅含小写字母/数字/连字符的 1-64 位字符串" >&2
    exit 1
}
[[ $version =~ ^[A-Za-z0-9_.-]+$ && $version != *..* ]] || {
    echo "无效的轻舟版本号: $version" >&2
    exit 1
}
library=$(cd "$library" 2>/dev/null && pwd) || {
    echo "产品库目录不存在: $3" >&2
    exit 1
}
template="$(cd "$(dirname "$0")/.." && pwd)/references/templates/pom.xml"
[ -f "$template" ] || {
    echo "缺少 POM 模板: $template" >&2
    exit 1
}

project="$root/qingzhou-app-$app"
mkdir -p "$project/src/main/java"

if [ ! -f "$project/pom.xml" ]; then
    sed -e "s/@APP@/$app/g" -e "s/@QINGZHOU_VERSION@/$version/g" "$template" > "$project/pom.xml"
fi

for entry in "qingzhou-api:modules" "qingzhou-json:components"; do
    artifact=${entry%%:*}
    directory=${entry##*:}
    source="$library/$directory/$artifact.jar"
    [ -f "$source" ] || {
        echo "缺少平台依赖: $source" >&2
        exit 1
    }
    repository="$project/.qingzhou-repository/qingzhou/$artifact/$version"
    mkdir -p "$repository"
    cp "$source" "$repository/$artifact-$version.jar"
    printf '<project><modelVersion>4.0.0</modelVersion><groupId>qingzhou</groupId><artifactId>%s</artifactId><version>%s</version></project>' \
        "$artifact" "$version" > "$repository/$artifact-$version.pom"
done

echo "工程目录: $project"
echo "源码路径: $project/src/main/java"
echo "已有源码与 POM 不会被覆盖。"
