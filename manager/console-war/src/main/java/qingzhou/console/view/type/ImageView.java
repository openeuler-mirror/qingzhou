package qingzhou.console.view.type;

import java.util.List;
import java.util.Map;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.crypto.CryptoService;

public class ImageView implements View {
    public static final String CONTENT_TYPE = "image/png";

    @Override
    public void render(RestContext restContext) throws Exception {
        List<Map<String, String>> dataList = restContext.request.getResponse().getDataList();
        if (dataList.isEmpty()) return;

        Map<String, String> map = dataList.get(0);
        String value = map.entrySet().iterator().next().getValue();
        CryptoService cryptoService = SystemController.getService(CryptoService.class);
        restContext.resp.getOutputStream().write(cryptoService.getBase64Coder().decode(value));
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }
}