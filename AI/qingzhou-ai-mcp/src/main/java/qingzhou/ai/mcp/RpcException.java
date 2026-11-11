package qingzhou.ai.mcp;

// 协议级错误：上抛后由 JsonRpc 统一装配为 JSON-RPC error，结果对象不承载错误语义
class RpcException extends Exception {
    // JSON-RPC 错误码；FORBIDDEN 为自定义区间，非批处理下由传输层同步回 HTTP 403
    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    static final int INTERNAL_ERROR = -32603;
    static final int FORBIDDEN = -32000;

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
