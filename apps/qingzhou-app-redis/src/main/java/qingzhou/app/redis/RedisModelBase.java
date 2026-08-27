package qingzhou.app.redis;

import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.app.redis.util.RedisUtil;

import java.util.Map;

public class RedisModelBase extends ModelBase {

    protected RedisUtil getRedisUtil() {
        RedisUtil util = RedisApp.getRedisUtil();
        if (util == null) {
            throw new RuntimeException("请先切换到一个 Redis 实例");
        }
        return util;
    }

    protected void fillBaseMetrics(Map<String, String> data, Map<String, String> info) {
        data.put("totalKeys", info.getOrDefault("total_keys", "0"));
        data.put("connectedClients", info.getOrDefault("connected_clients", "0"));
        data.put("hitRate", info.getOrDefault("hit_rate", "0"));
    }

    protected boolean isProduction() {
        String envType = RedisApp.getCurrentEnvType();
        return "production".equals(envType);
    }

    protected boolean isTest() {
        String envType = RedisApp.getCurrentEnvType();
        return "test".equals(envType);
    }

    protected String getCurrentEnvType() {
        return RedisApp.getCurrentEnvType();
    }








    protected boolean checkWriteAllowed(Request request) {
        String envType = getCurrentEnvType();
        if (envType == null || "development".equals(envType)) {
            return true;
        }

        String confirmed = request.getParameter("_env_confirmed");
        if ("true".equals(confirmed)) {
            return true;
        }

        String msg = "production".equals(envType)
                ? "⚠️ 当前为生产环境，确认执行此写操作？"
                : "当前为测试环境，确认执行此写操作？";
        request.getResponse()
                .success(false)
                .message(msg)
                .messageLevel(Response.MessageLevel.warn);
        return false;
    }








    protected boolean checkDangerousAllowed(Request request) {
        String envType = getCurrentEnvType();
        if (envType == null || "development".equals(envType)) {
            return true;
        }

        String confirmed = request.getParameter("_env_confirmed");
        if ("true".equals(confirmed)) {
            return true;
        }

        String msg = "production".equals(envType)
                ? "🚫 当前为生产环境，确认执行此危险操作？此操作不可恢复！"
                : "⚠️ 当前为测试环境，确认执行此危险操作？";
        request.getResponse()
                .success(false)
                .message(msg)
                .messageLevel(Response.MessageLevel.warn);
        return false;
    }








    protected void checkWriteAllowed() throws Exception {
        String envType = getCurrentEnvType();
        if ("production".equals(envType) || "test".equals(envType)) {
            throw new Exception("当前环境下禁止此写操作");
        }
    }








    protected void checkDangerousAllowed() throws Exception {
        String envType = getCurrentEnvType();
        if ("production".equals(envType) || "test".equals(envType)) {
            throw new Exception("当前环境下禁止此危险操作");
        }
    }
}
