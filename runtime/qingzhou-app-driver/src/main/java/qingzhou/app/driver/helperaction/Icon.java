package qingzhou.app.driver.helperaction;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import qingzhou.app.driver.HelperAction;
import qingzhou.common.FileUtil;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;

public class Icon implements HelperAction {
    private final Crypto crypto;
    private final String iconPath;
    private volatile String iconCache;

    public Icon(Crypto crypto, String iconPath) {
        this.crypto = crypto;
        this.iconPath = iconPath;
    }

    @Override
    public void execute(RequestImpl request) throws Exception {
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
        if (iconPath == null) return null;

        try (InputStream resource = this.getClass().getResourceAsStream(iconPath)) {
            if (resource == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileUtil.copyStream(resource, bos);
            return crypto.getBase64Coder().encode(bos.toByteArray());
        }
    }
}
