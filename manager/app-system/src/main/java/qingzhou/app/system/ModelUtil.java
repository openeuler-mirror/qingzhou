package qingzhou.app.system;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;

import java.util.*;

public class ModelUtil {
    public static void invokeOnAgent(Request request, String... instance) {
        String originModel = request.getModel();
        RequestImpl requestImpl = (RequestImpl) request;
        try {
            requestImpl.setModelName(DeployerConstants.MODEL_AGENT);
            List<Response> responseList = Main.getService(ActionInvoker.class)
                    .invokeOnInstances(request, instance);
            if (responseList.size() == 1) {
                requestImpl.setResponse(responseList.get(0));
            } else {
                throw new IllegalStateException();
            }
        } finally {
            requestImpl.setModelName(originModel);
        }
    }

    public static List<Map<String, String>> listData(String[] allIds, Supplier supplier,
                                                     int pageNum, int pageSize, String[] fieldNames) throws Exception {
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        List<Map<String, String>> data = new ArrayList<>();
        for (String id : subList) {
            Map<String, String> result = new HashMap<>();

            Map<String, String> idData = supplier.get(id);
            for (String fieldName : fieldNames) {
                result.put(fieldName, idData.get(fieldName));
            }

            data.add(result);
        }
        return data;
    }

    public interface Supplier {
        Map<String, String> get(String id) throws Exception;
    }
}
