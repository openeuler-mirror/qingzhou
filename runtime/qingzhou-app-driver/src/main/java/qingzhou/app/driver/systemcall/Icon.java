package qingzhou.app.driver.systemcall;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import qingzhou.app.driver.AppContextImpl;
import qingzhou.app.driver.FileUtil;
import qingzhou.app.driver.SystemCall;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;

public class Icon implements SystemCall {
    private final AppContextImpl appContext;
    private volatile String iconCache;

    public Icon(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    @Override
    public void call(RequestImpl request) throws Exception {
        if (iconCache == null) {
            synchronized (this) {
                if (iconCache == null) {
                    iconCache = loadIcon();
                }
            }
        }
        if (iconCache == null) {
            request.getResponse().success(false);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("icon", iconCache);
        data.put("encoding", "base64");
        request.getResponse().data(data);
    }

    private String loadIcon() throws Exception {
        String iconPath = appContext.appMeta.getApp().icon;
        try (InputStream resource = this.getClass().getResourceAsStream(iconPath)) {
            if (resource == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileUtil.copyStream(resource, bos);
            byte[] bytes = bos.toByteArray();
            Crypto crypto = appContext.getService(Crypto.class);
            Base64Coder base64Coder = crypto.getBase64Coder();
            return base64Coder.encode(bytes);
        }
    }
}
