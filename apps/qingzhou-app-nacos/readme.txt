1. 使用：执行mvn clean install，将jar包放到qingzhou apps下即可。需要有nacos启动着的。
2. 功能：管理nacos命名空间和配置等
3. 限制：当前仅支持nacos 2.x版本
4. 默认配置：
nacos.serverAddr=http://localhost:8848
nacos.username=nacos
nacos.password=nacos
可在qingzhou.properties给当前应用修改默认配置。
app~qingzhou-app-nacos.nacos.serverAddr=http://localhost:8848
app~qingzhou-app-nacos.nacos.username=nacos
app~qingzhou-app-nacos.nacos.password=nacos