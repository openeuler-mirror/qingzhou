# Qingzhou Web Frontend

前端静态资源服务模块，用于将 Qingzhou Web UI 打包到后端一起部署。

## 目录结构

```
qingzhou-web-frontend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── qingzhou/
│   │   │       └── web/
│   │   │           └── frontend/
│   │   │               └── StaticResourceServer.java  # 静态资源处理器
│   │   └── resources/
│   │       └── webapp/                                 # 前端静态资源目录
│   │           ├── index.html
│   │           ├── assets/
│   │           └── ...
│   └── test/
├── pom.xml
└── README.md
```

## 使用方法

### 1. 构建前端项目

```bash
cd qingzhou-web-ui
npm run build
```

### 2. 复制静态资源

将前端构建好的 `dist` 目录内容复制到本模块的 `src/main/resources/webapp/` 目录下：

```bash
cp -r qingzhou-web-ui/dist/* qingzhou-web-frontend/src/main/resources/webapp/
```

## 注意事项

1. 前端更新后需要重新复制静态资源并打包
2. 支持热部署，替换 jar 包后自动生效
3. 所有非 `/web/` 和 `/invoke/` 开头的请求都会由本服务处理
