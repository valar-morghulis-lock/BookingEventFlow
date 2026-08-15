package com.bookingeventflow.common.pagination;

public interface CursorCodec<T> {

    String encode(T cursor);

    T decode(String value);
}