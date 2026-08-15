package com.bookingeventflow.common.pagination;

public class CursorDecodingException
        extends RuntimeException {

    public CursorDecodingException(String message) {
        super(message);
    }

    public CursorDecodingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}