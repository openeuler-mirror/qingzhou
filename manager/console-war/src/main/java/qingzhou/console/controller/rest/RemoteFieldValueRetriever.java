package qingzhou.console.controller.rest;

import java.util.Map;

import qingzhou.api.type.Show;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;

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
            ResponseImpl response = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
            originData = (Map<String, String>) response.getInternalData();
        }

        return originData.get(fieldName);
    }
}