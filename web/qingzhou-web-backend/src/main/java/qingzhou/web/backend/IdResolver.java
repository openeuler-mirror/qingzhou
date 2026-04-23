package qingzhou.web.backend;

class IdResolver {
    static String toAppId(String instanceId, String appCode) {
        return appCode + "@" + instanceId;
    }

    static String[] fromAppId(String appId) {
        String[] split = appId.split("@");
        if (split.length != 2) return null;
        return new String[]{split[1], split[0]};
    }

    static String toModelId(String instanceId, String appCode, String modelCode) {
        return modelCode + "@" + appCode + "@" + instanceId;
    }

    static String[] fromModelId(String modelId) {
        String[] split = modelId.split("@");
        if (split.length != 3) return null;
        return new String[]{split[2], split[1], split[0]};
    }
}
