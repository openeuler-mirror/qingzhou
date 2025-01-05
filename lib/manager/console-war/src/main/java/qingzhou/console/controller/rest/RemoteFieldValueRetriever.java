package qingzhou.console.controller.rest;

import java.util.Map;

import qingzhou.api.type.Show;
import qingzhou.console.controller.SystemController;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;

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
            ResponseImpl response = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeAny(tmp);
            originData = (Map<String, String>) response.getInternalData();
        }

        return originData.get(fieldName);
    }
}