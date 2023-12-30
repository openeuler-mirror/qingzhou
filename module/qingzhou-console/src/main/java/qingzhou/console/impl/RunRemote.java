package qingzhou.console.impl;

import qingzhou.console.servlet.RequestContext;
import qingzhou.console.servlet.ServletProcessor;
import qingzhou.console.servlet.UploadFileContext;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.util.Constants;
import qingzhou.framework.util.FileUtil;
import qingzhou.console.SecureKey;
import qingzhou.framework.util.StreamUtil;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.SerializerService;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

public class RunRemote implements Process {
    private final Controller controller;

    public RunRemote(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void exec() {
        if (controller.isMaster) return;

        ServletProcessor processor = new ServletProcessorImpl();
        controller.servletService.addSingleServletWebapp(Constants.remoteApp, "/*", controller.frameworkContext.getCache().getAbsolutePath(), processor);
    }

    @Override
    public void undo() {
        if (controller.isMaster) return;

        controller.servletService.removeApp(Constants.remoteApp);
    }

    private class ServletProcessorImpl implements ServletProcessor {
        private void process0(InputStream in, OutputStream out) throws Exception {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(in.available());
            StreamUtil.copyStream(in, bos);
            byte[] requestData = bos.toByteArray();

            String remoteKey = SecureKey.getOrInitKey(ConsoleWarHelper.getDomain(), SecureKey.remoteKeyName);
            PasswordCipher passwordCipher = ConsoleWarHelper.getPasswordCipher(remoteKey);
            byte[] decryptedData = passwordCipher.decrypt(requestData);

            Serializer serializer = controller.frameworkContext.getService(SerializerService.class).getSerializer();
            Request request = serializer.deserialize(decryptedData, RequestImpl.class);

            Response response = new ResponseImpl();

            AppManager appManager = controller.frameworkContext.getAppInfoManager();
            AppInfo appInfo = appManager.getAppInfo(request.getAppName());
            appInfo.invokeAction(request, response);

            byte[] responseData = serializer.serialize(response);
            byte[] encryptData = passwordCipher.encrypt(responseData);

            ObjectOutput objectOutput = new ObjectOutputStream(out);
            objectOutput.writeInt(encryptData.length);
            objectOutput.write(encryptData);// 不用 writeObject() 避免反序列化漏洞
            objectOutput.flush();
        }

        @Override
        public void process(RequestContext requestContext) {
            try {
                String requestURI = requestContext.getRequestURI();
                if (requestURI.equals(Constants.remoteApp + Constants.uploadPath)) { // todo 需要增加和 process0 一致的安全认证
                    UploadFileContext uploadFile = requestContext.getUploadContext();
                    String submittedFileName = uploadFile.getFileName();
                    File file = FileUtil.newFile(ConsoleWarHelper.getUploadDir(), submittedFileName);
                    try (ReadableByteChannel readChannel = Channels.newChannel(uploadFile.getInputStream());
                         FileChannel writeChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                        writeChannel.transferFrom(readChannel, 0, uploadFile.getSize());
                    }
                    OutputStream out = requestContext.getOutputStream();
                    out.write(file.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    out.close();
                } else if (requestURI.equals(Constants.remoteApp + Constants.deleteFilePath)) { // todo 需要增加和 process0 一致的安全认证
                    String[] values = requestContext.getParameterValues("deleteFiles");
                    if (values != null) {
                        for (String value : values) {
                            File file = FileUtil.newFile(value);
                            if (file.exists()) {
                                FileUtil.delete(file);
                            }
                        }
                    }
                } else {
                    process0(requestContext.getInputStream(), requestContext.getOutputStream());
                }
            } catch (Throwable e) {
                Throwable t = e;
                while (t instanceof InvocationTargetException && t.getCause() != null) {
                    t = t.getCause();
                }
                throw new RuntimeException(t);
            }
        }
    }
}
