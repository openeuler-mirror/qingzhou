package qingzhou.http.impl;

class BodyTooLargeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    BodyTooLargeException() {
        super("request body too large");
    }
}
