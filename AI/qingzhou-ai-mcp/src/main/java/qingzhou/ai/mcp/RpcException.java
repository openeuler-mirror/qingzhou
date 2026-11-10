package qingzhou.ai.mcp;

// 协议级错误：上抛后由 process 统一装配为 JSON-RPC error，结果对象不承载错误语义
class RpcException extends RuntimeException {
    final int code;
    final Object data;

    RpcException(int code, String message) {
        this(code, message, null);
    }

    RpcException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}