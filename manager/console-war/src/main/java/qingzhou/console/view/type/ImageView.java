package qingzhou.console.view.type;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.crypto.CryptoService;

import java.util.List;
import java.util.Map;

public class ImageView implements View {
    public static final String CONTENT_TYPE = "image/png";

    @Override
    public void render(RestContext restContext) throws Exception {
        List<Map<String, String>> dataList = restContext.request.getResponse().getDataList();
        if (dataList.isEmpty()) return;

        Map<String, String> map = dataList.get(0);
        String value = map.entrySet().iterator().next().getValue();
        CryptoService cryptoService = SystemController.getService(CryptoService.class);
        restContext.servletResponse.getOutputStream().write(cryptoService.getHexCoder().hexToBytes(value));
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }
}