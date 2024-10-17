package qingzhou.console.controller.rest;

import qingzhou.api.Response;
import qingzhou.api.type.Show;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.RequestImpl;

import java.util.HashMap;
import java.util.Map;

class RemoteFieldValueRetriever implements SecurityController.FieldValueRetriever {
    private final String id;
    private final RequestImpl request;
    private Map<String, String> originData;

    RemoteFieldValueRetriever(String id, RequestImpl request) {
        this.id = id;
        this.request = request;
    }

    @Override
    public String getFieldValue(String fieldName) {
        if (originData == null) {
            RequestImpl tmp = new RequestImpl(request);
            tmp.setActionName(Show.ACTION_SHOW);
            tmp.setId(id);
            Response response = SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
            if (!response.getDataList().isEmpty()) {
                originData = response.getDataList().get(0);
            } else {
                originData = new HashMap<>();
            }
        }

        return originData.get(fieldName);
    }
}